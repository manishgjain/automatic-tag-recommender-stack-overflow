name := "stack-overflow-tag-recommender"

version := "1.0"

scalaVersion := "2.10.2"


libraryDependencies <<= scalaVersion {
  scala_version => Seq(
    // Spark and Mllib
    "org.apache.spark" %% "spark-core" % "1.0.2",
    "org.apache.spark" %% "spark-mllib" % "1.1.0",
    // Lucene
    "org.apache.lucene" % "lucene-core" % "4.6.1",
    // for Porter Stemmer
    "org.apache.lucene" % "lucene-analyzers-common" % "4.6.1",
    // Guava for the dictionary
    "com.google.guava" % "guava" % "17.0",
    // article extractor
    "com.gravity" %% "goose" % "2.1.23"
  )
}

// used for goose
resolvers += Resolver.mavenLocal
