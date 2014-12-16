This project involves 4 modules. 

1-- Data cleaning module - Python

2-- Data indexing and segementation module - java - lucene

3-- SVM Classifier module - scala - spark

4-- Hybrid Kmeans + SVM classifier module - scala - spark


The last 3 modules are driven by the root directory &lt;root-dir&gt;

Step 1: Download the stackoverflow data from http://tejp.de/files/so/dbdump/

Step 2: Place the posts.xml and tags.xml in &lt;root-dir&gt; and run the python scripts from the same directory.

Step 3: Run module 2 with the command line arguments -index &lt;root-dir&gt;. It will index all the Questions in the &lt;root-dir&gt;/data and place the index files in &lt;root-dir&gt;/indexDir

Step 3: Run module 2 again with arguments -segmentFullData &lt;root-dir&gt;. It will perform the required data segmentation. See the report.pdf for more details about this step.

Step 4: Run module 3 with arguments -train &lt;root-dir&gt; for training the SVM models for each tag.

Step 5: For lanching the console for SVM only classifiers, run module 3 with arguments -console &lt;root-dir&gt; and follow the on screen instructions.

Step 6: Run module 4 for learning the Kmeans model with arguments -cluster &lt;root-dir&gt; &lt;k&gt;

Step 7: Run module 2 with argument -perClusterTags to generate the per cluster tags list to be used in the console for KMeans module.

Step 8: FOr launching the console for KM-SVM classifiers, run module 4 with arguments -console &lt;root-dir&gt; and follow the on screen instructions.


This project is still in development and we are trying to improve it further. For suggestions, improvements and contributions, please contact Manish Jain @ manishjain181289@gmail.com or Namrata Bikhchandani @ namrata0617@gmail.com



