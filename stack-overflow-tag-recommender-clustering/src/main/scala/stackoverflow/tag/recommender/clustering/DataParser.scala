package stackoverflow.tag.recommender.clustering

import scala.io.Source
import scala.collection.mutable

object DataParser {
  var currentLabel: String = null
  var currentIntegerLabel: Int = -1

  def parseAll(dataFiles: Iterable[String]) = dataFiles flatMap parse

  def parse(dataFile: String) = {
    val docs = mutable.ArrayBuffer.empty[Document]
    println("@@@@@@ "+dataFile)
    var currentDoc: Document = null
    if(dataFile.contains("positive")) {
        currentLabel = "positive"
	currentIntegerLabel = 1
    }
    if(dataFile.contains("negative")) {
        currentLabel = "negative"
	currentIntegerLabel = 0
    }
    for (line <- Source.fromFile(dataFile).getLines()) {
	var lineContents = line.split("\\t");
	var temp: Set[String] = Set.empty
	temp +=currentLabel
        currentDoc = Document(lineContents(0), lineContents(1)+" "+lineContents(2), temp, currentIntegerLabel)
	docs += currentDoc
    }
    docs
  }
}

case class Document(docId: String, body: String = "", label: Set[String] = Set.empty, integerLabel: Int)
