import csv;

with open('Tags.csv','rb') as csvfile:
    tags = csv.reader(csvfile,delimiter=',')
    tagFile = open('tags_filtered.txt','wb')
    count = 0
    for row in tags:
        if int(row[2]) > 2000 :
            count+=1
            tagFile.write(row[1]+'\n')
    tagFile.close()
    print count