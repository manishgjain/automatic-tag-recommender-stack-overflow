package stackoverflow.tag.recommender.clustering

import java.io._
import scala.io.Source
import jline.ConsoleReader
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.{NaiveBayes, NaiveBayesModel, SVMModel, SVMWithSGD}
import org.apache.spark.mllib.evaluation.BinaryClassificationMetrics
import org.apache.spark.mllib.linalg.Vectors
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.clustering.KMeans
import org.apache.spark.mllib.clustering.KMeansModel
import java.io.PrintWriter

object DataClusteringModule extends App {

	var rootDir : String = null
	var clusterCount = 0
	var sc : SparkContext =  null 
	
	if(args.length == 4 && args(0).equals("-cluster")) {
		//set root path and train the classifiers.
		rootDir = args(1)
		clusterCount = args(2).toInt
		sc = new SparkContext(args(3), "Data Clustering App")
		clusterData(rootDir, clusterCount)
	}
	else if(args.length == 3 && args(0).equals("-console")) {
		//set root path to deserialize classfier models and launch console
		val rootDir = args(1)
		sc = new SparkContext(args(2), "Data Clustering App")
		launchConsole(rootDir)
	}
	else {
		println("Usage: -cluster <root Dir Of project data> <k> <spark-master-url>")
		sc.stop();
		exit()
	}
	
	def clusterData(rootDir : String, clusterCount : Int) = {
		val tagsFile = rootDir + "/" + "tags_filtered.txt";
		var tagsList : List[String] = List()
		for(line <- Source.fromFile(tagsFile).getLines()) {
			tagsList ::= line.trim()
		}
		
		val labeledPointsAndDictionaries = createLabeledVectors(rootDir, tagsList)
		val iterationCount = 1000
		val kmeans = new KMeans().setK(clusterCount)
					.setMaxIterations(iterationCount)
					.setRuns(3)
					.setInitializationMode("k-means||")
					.setEpsilon(0.000001)
					.run(labeledPointsAndDictionaries.points.map(_.features))
		
		//Maps docId to ClusterId
		mapDocIdToClusterId(labeledPointsAndDictionaries, kmeans)
		//Serialize the kMeansModelAndDictionaries to filesystem.
		val kMeansModelAndDictionaries = KMeansModelAndDictionaries(kmeans, labeledPointsAndDictionaries.termDictionary, labeledPointsAndDictionaries.docIdDictionary, labeledPointsAndDictionaries.idfs)
		val fileOutputStream = new FileOutputStream(rootDir + "/kMeansModel/" + "kMeans.obj")
		val objectOutputStream = new ObjectOutputStream(fileOutputStream)
		objectOutputStream.writeObject(kMeansModelAndDictionaries)
		objectOutputStream.close()
		sc.stop()
	}


	def predictCluster(kMeansModelAndDictionaries: KMeansModelAndDictionaries, content: String) = {
		// tokenize content and stem it
		val tokens = Tokenizer.tokenize(content)
		// compute TFIDF vector
		val tfIdfs = kMeansModelAndDictionaries.termDictionary.tfIdfs(tokens, kMeansModelAndDictionaries.idfs)
		val vector = kMeansModelAndDictionaries.termDictionary.vectorize(tfIdfs)
		//Compute the raw score of the new vector
		val clusterId = kMeansModelAndDictionaries.kMeansModel.predict(vector)
		
	        val tagsFile = rootDir + "/cluster/" + "cluster_" + clusterId + ".txt";
                var tagsList : List[String] = List()
                for(line <- Source.fromFile(tagsFile).getLines()) {
                        tagsList ::= line.trim()
                }
		
                var models : Map[String, ClassifierModelAndDictionaries] = Map()
                for(tag <- tagsList) {
                        models += (tag -> readSVMModel(rootDir, tag))
                }
		predictAll(models, content)
	}

	def predictAll(models : Map[String, ClassifierModelAndDictionaries], content : String) = {
		var scoresList : Map[String, Double] = Map()
		for(tag <- models.keySet) {
			scoresList += (tag -> predict(models.getOrElse(tag, null), content))
		}
		val finalList = scoresList.toList sortBy{-_._2} slice(0,5)
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

	def readSVMModel(rootDir : String, tag : String) : ClassifierModelAndDictionaries = {
		val fileInputStream = new FileInputStream(rootDir + "/modelObjectData/" + tag + ".obj")
		val objectInputStream = new ObjectInputStream(fileInputStream)
		return objectInputStream.readObject().asInstanceOf[ClassifierModelAndDictionaries]
	}

	def readKMeansModel(rootDir : String) : KMeansModelAndDictionaries = {
                val fileInputStream = new FileInputStream(rootDir + "/kMeansModel/" + "kMeans.obj")
                val objectInputStream = new ObjectInputStream(fileInputStream)
                return objectInputStream.readObject().asInstanceOf[KMeansModelAndDictionaries]
        }

	def launchConsole(rootDir : String ) = {
		var kMeansModel = readKMeansModel(rootDir)

                println("Enter 'q' to quit.\nEnter Question Title and Body separated by whitespace.\n")
                val consoleReader = new ConsoleReader()
                while ( {
                        consoleReader.readLine("Question> ") match {
                                case s if s == "q" =>   false
                                case content: String => predictCluster(kMeansModel, content)
							true
                                case _ =>               true
                        }
                }) {}
                sc.stop()
        }
	
	def createLabeledVectors(rootDir : String, tags : List[String]) : LabeledPointsAndDictionaries = {
		var inputFiles : List[String] = List()
		for(tag <- tags) {
			val trainFilePath = rootDir + "/trainData/" + tag
			var currentFileName = (new File(trainFilePath)).list(new FilenameFilter {
				override def accept(dir: File, name: String) = name.endsWith("positive.tsv")
			})(0)
			inputFiles = (trainFilePath + "/" + currentFileName) :: inputFiles
			
		} 

		val docs = DataParser.parseAll(inputFiles)
		val termDocs = Tokenizer.tokenizeAll(docs)

		//Put the input data into spark clusters
		val termDocsRdd = sc.parallelize[TermDoc](termDocs.toSeq)
		
		//Find the total number of documents
		val numDocs = termDocs.size
		
		//Find the term dictionary i.e. set of all unique words in the data
		val terms = termDocsRdd.flatMap(_.terms).distinct().collect().sortBy(identity)
		val termDict = new Dictionary(terms)
		
		//Similarly, find the label dictionary. It contains either positive or negative as labels
		val docIds = termDocsRdd.flatMap(_.doc).distinct().collect()
		val docIdDict = new Dictionary(docIds)

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
		val tfidfs = termDocsRdd flatMap {
			termDoc =>
				val termPairs = termDict.tfIdfs(termDoc.terms, idfs)
				termDoc.doc.headOption.map {
					docId =>
						val docIdIndex = docIdDict.indexOf(docId).toDouble
						val vector = Vectors.sparse(termDict.count, termPairs)
						LabeledPoint(docIdIndex, vector)
        			}
			}
		return LabeledPointsAndDictionaries(tfidfs, termDict, docIdDict, idfs)
	}
	
	def mapDocIdToClusterId(labeledPointsAndDictionaries : LabeledPointsAndDictionaries, kMeansModel : KMeansModel) = {
	  
		val docIdDict = labeledPointsAndDictionaries.docIdDictionary
		val clusterMap = labeledPointsAndDictionaries.points.map { 
						point => 
							val clusterId = kMeansModel.predict(point.features)
							val docId = docIdDict.valueOf(point.label.toInt)
							(clusterId, docId)
		}
		clusterMap.saveAsTextFile(rootDir + "/generativeModel/clusters")
	}

}
case class ClusterIdAndDocIdTuple( clusterId : Int, docId : String) extends Serializable

case class KMeansModelAndDictionaries( kMeansModel : KMeansModel, 
			   termDictionary : Dictionary, 
			   docIdDictionary : Dictionary,
			   idfs : Map[String, Double]) extends Serializable

			   
case class LabeledPointsAndDictionaries( points : RDD[LabeledPoint], 
			   termDictionary : Dictionary, 
			   docIdDictionary : Dictionary,
			   idfs : Map[String, Double]) extends Serializable

case class ClassifierModelAndDictionaries( svmModel : SVMModel = null, 
			termDictionary : Dictionary = null, 
			labelDictionary : Dictionary = null, 
			idfs : Map[String, Double] = null, 
			accuracy : Option[Double]) extends Serializable
