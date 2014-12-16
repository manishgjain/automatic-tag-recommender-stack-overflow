package stackoverflow.tag.recommender

import java.io._
import scala.io.Source
import jline.ConsoleReader
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.{NaiveBayes, NaiveBayesModel, SVMModel, SVMWithSGD}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint

object DiscriminativeModel extends App {

	var sc : SparkContext = null
	if(args.length == 3 && args(0).equals("-train")) {
		//set root path and train the classifiers.
		val rootDir = args(1)
		sc = new SparkContext(args(2), "discriminative Model")
		trainClassifiers(rootDir)
	}
	else if(args.length == 3 && args(0).equals("-console")) {
		//set root path to deserialize classfier models and launch console
		val rootDir = args(1)
		sc = new SparkContext(args(2), "discriminative Model")
		launchConsole(rootDir)
	}
	else if(args.length == 3 && args(0).equals("-reportPerformance")) {
		val rootDir = args(1)
		sc = new SparkContext(args(2), "discriminative Model")
		trainAndReportPerformance(rootDir)
	}
	else {
		println("Usage: [-train|-console|-reportPerformance] <root Dir Of project data> <Master URL of Spark>")
		exit()
	}
	
	def trainClassifiers(rootDir : String) = {
		val tagsFile = rootDir + "/" + "tags_filtered.txt";
		var tagsList : List[String] = List()
		for(line <- Source.fromFile(tagsFile).getLines()) {
			tagsList ::= line.trim()
		}
		
		var models : Map[String, ClassifierModelAndDictionaries] = Map()
		for(tag <- tagsList) {
			models += (tag -> createSVMModel(rootDir, tag, false))
		}

		models.foreach {
			modelData => {
				val fileOutputStream = new FileOutputStream(rootDir + "/modelObjectData/" + modelData._1 + ".obj")
				val objectOutputStream = new ObjectOutputStream(fileOutputStream)
				objectOutputStream.writeObject(modelData._2)
				objectOutputStream.close()
			}
		}				
		sc.stop()
	}

	def trainAndReportPerformance(rootDir : String) = {
		val tagsFile = rootDir + "/" + "tags_filtered.txt";
		var tagsList : List[String] = List()
		for(line <- Source.fromFile(tagsFile).getLines()) {
			tagsList ::= line.trim()
		}
		
		var models : Map[String, ClassifierModelAndDictionaries] = Map()
		for(tag <- tagsList) {
			models += (tag -> createSVMModel(rootDir, tag, true))
		}

		val performanceReportFile = new File(rootDir + "/performanceReport.txt")
		val bufferedWriter = new BufferedWriter(new FileWriter(performanceReportFile))
		bufferedWriter.write("TagName\tAccuracy\n")
		models.foreach { modelData => bufferedWriter.write(modelData._1 + "\t" + modelData._2.accuracy + "\n") }
		bufferedWriter.close()
		sc.stop()
	}

	def createSVMModel(rootDir : String, tag : String, reportPerformance : Boolean) : ClassifierModelAndDictionaries = {
		println("Create SVM Model for " + tag + " in " + rootDir + "/" + tag)
		val trainFilePath = rootDir + "/trainData/" + tag
		val inputFiles = new File(trainFilePath).list(new FilenameFilter {
			override def accept(dir: File, name: String) = name.startsWith(tag)
		})

		val fullFileNames = inputFiles.map(trainFilePath + "/" + _)
		val docs = DataParser.parseAll(fullFileNames)
		val termDocs = Tokenizer.tokenizeAll(docs)

		//Put the input data into spark clusters
		val termDocsRdd = sc.parallelize[TermDoc](termDocs.toSeq)
		
		//Find the total number of documents
		val numDocs = termDocs.size
		
		//Find the term dictionary i.e. set of all unique words in the data
		val terms = termDocsRdd.flatMap(_.terms).distinct().collect().sortBy(identity)
		val termDict = new Dictionary(terms)
		
		//Similarly, find the label dictionary. It contains either positive or negative as labels
		val labels = termDocsRdd.flatMap(_.label).distinct().collect()
		val labelDict = new Dictionary(labels)

		//find the inverse document frequency of the input documents
		val idfs = (termDocsRdd.flatMap( 
				termDoc => termDoc.terms.map((termDoc.doc, _))).distinct().groupBy(_._2) collect {
					// if term is present in less than 3 documents then remove it
					case (term, docs) if docs.size > 3 =>
					term -> (numDocs.toDouble / docs.size.toDouble)
				}
			   ).collect.toMap

		//Find the term frequency - inverse document frequency weighted vectors for the input documents
		//And initialize the labelled points of (label and vector pair) needed by the mllib classifiers.
		var tfidfs = termDocsRdd flatMap {
			termDoc =>
				val termPairs = termDict.tfIdfs(termDoc.terms, idfs)
				termDoc.label.headOption.map {
					label =>
						val labelId = labelDict.indexOf(label).toDouble
						val vector = Vectors.sparse(termDict.count, termPairs)
						LabeledPoint(labelId, vector)
        			}
			}
		if(!reportPerformance) {
			tfidfs = tfidfs.cache()
			return ClassifierModelAndDictionaries(SVMWithSGD.train(tfidfs, 20), termDict, labelDict, idfs, None)
		}
		else {
			val split = tfidfs.randomSplit(Array(0.80,0.20),seed = 11L)
			val training = split(0).cache()
			val testing = split(1)
			
			var model = SVMWithSGD.train(training, 20)
			val predictionAndLabel = testing.map(p => (model.predict(p.features), p.label))			
			val accuracy = 1.0 * predictionAndLabel.filter(x => x._1 == x._2).count() / testing.count()
			return ClassifierModelAndDictionaries(model,  termDict, labelDict, idfs, Some(accuracy))
		}
	}

	def launchConsole(rootDir : String ) = {
		val tagsFile = rootDir + "/" + "tags_filtered.txt";
		var tagsList : List[String] = List()
		for(line <- Source.fromFile(tagsFile).getLines()) {
			tagsList ::= line.trim()
		}
		
		var models : Map[String, ClassifierModelAndDictionaries] = Map()
		for(tag <- tagsList) {
			models += (tag -> readSVMModel(rootDir, tag))
		}
	
		println("Enter 'q' to quit.\nEnter Question Title and Body separated by whitespace.\n")
		val consoleReader = new ConsoleReader()
		while ( {
			consoleReader.readLine("Question> ") match {
				case s if s == "q" => 	false
				case content: String => predictAll(models, content)
							true
				case _ => 		true
			}
		}) {}
		sc.stop()
	}

	def readSVMModel(rootDir : String, tag : String) : ClassifierModelAndDictionaries = {
		val fileInputStream = new FileInputStream(rootDir + "/modelObjectData/" + tag + ".obj")
		val objectInputStream = new ObjectInputStream(fileInputStream)
		return objectInputStream.readObject().asInstanceOf[ClassifierModelAndDictionaries]
	}

	def predictAll(models : Map[String, ClassifierModelAndDictionaries], content : String) = {
		var scoresList : Map[String, Double] = Map()
		for(tag <- models.keySet) {
			scoresList += (tag -> predict(models.getOrElse(tag, null), content))
		}
		val finalList = scoresList.toList sortBy{-_._2} slice(0,10)
		println("Tags : " + finalList.toString())
	}

	def predict(classifierModelAndDictionaries: ClassifierModelAndDictionaries, content: String) : Double = {
		// tokenize content and stem it
		val tokens = Tokenizer.tokenize(content)
		// compute TFIDF vector
		val tfIdfs = classifierModelAndDictionaries.termDictionary.tfIdfs(tokens, classifierModelAndDictionaries.idfs)
		val vector = classifierModelAndDictionaries.termDictionary.vectorize(tfIdfs)
		//Compute the raw score of the new vector
		val rawScore = classifierModelAndDictionaries.svmModel.clearThreshold().predict(vector)
		
		rawScore
	}

}
case class ClassifierModelAndDictionaries(svmModel : SVMModel = null, termDictionary : Dictionary = null, labelDictionary : Dictionary = null, idfs : Map[String, Double] = null, accuracy : Option[Double]) extends Serializable
