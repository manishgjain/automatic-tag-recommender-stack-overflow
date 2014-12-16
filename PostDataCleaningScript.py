#Post file conversion
import xml.etree.ElementTree as ET
import re;
import os;
 
XMLFileName = 'Posts.xml'
NewXmlFileNamePrefix = 'Questions_'
Path = 'data'
NewXmlFileNameExtension = '.xml'  
MAX_QUESTIONS = 200000
# XMLFileName = 'Posts_small_1.xml'
# NewXmlFileNamePrefix = 'Questions_small_'
# Path = 'data_small'

if not os.path.exists(Path):
    os.mkdir(Path)

tagfile = open('tags_filtered.txt','rb')
tagSet = set()
for tag in tagfile:
    tagSet.add(tag.strip())

with open(XMLFileName,'rb') as f:
    questionCounter = 0
    fileNumber = 0
    newXmlFile = open(os.path.join(Path,NewXmlFileNamePrefix+str(fileNumber)+NewXmlFileNameExtension),'wb')
    newXmlFile.write('<questions>\n')
    for line in f:
        if "posts>" not in line and "<?xml" not in line:
            postTree = ET.fromstring(line)
            questionPostTypeId = postTree.attrib['PostTypeId']
            validQuestion = False
            if questionPostTypeId == '1':
                #Get all the required attributes from Input XML tree into local variables
                questionId = postTree.attrib['Id']
                
                questionTitle = postTree.attrib['Title']
                cleanedQuestionTitle, numberReplaced = re.subn("\<(([A-Za-z]*)|(\/[A-Za-z]*))\>", " ", questionTitle)
                
                questionBody = postTree.attrib['Body']
                cleanedQuestionBody, numberReplaced = re.subn("&#10|&#xD;|&#xA;|\\n|\\r", "", questionBody)
                cleanedQuestionBody, numberReplaced = re.subn("\<([^&]*?)\>", "", cleanedQuestionBody)
                cleanedQuestionBody, numberReplaced = re.subn("&gt;|&lt;([\/]?)", "", cleanedQuestionBody)
                
                questionAnsCount = postTree.attrib['AnswerCount']
                
                questionViewCount = postTree.attrib['ViewCount']
                
                questionTags = postTree.attrib['Tags']
                cleanedQuestionTags, numberReplaced = re.subn("&lt;|<", "", questionTags)
                cleanedQuestionTags, numberReplaced = re.subn("&gt;|>", ",", cleanedQuestionTags)
                questionTagSet = set(cleanedQuestionTags.split(','))
                questionTagSet.remove('')
                if questionTagSet.issubset(tagSet):
                    validQuestion = True
                    #Set the extracted attributed above from local variables into tags of the output XML tree
                    root = ET.Element('question')
                    
                    qId = ET.SubElement(root, 'Id')
                    qId.text = questionId
                    
                    qTitle = ET.SubElement(root, 'Title')
                    qTitle.text = cleanedQuestionTitle
                    
                    qBody = ET.SubElement(root, 'Body')
                    qBody.text = cleanedQuestionBody
                    
                    qTags = ET.SubElement(root, 'Tags')
                    qTags.text = cleanedQuestionTags
                    
                    qAnsCount = ET.SubElement(root, 'AnsCount')
                    qAnsCount.text = questionAnsCount
                    
                    qViewCount = ET.SubElement(root, 'ViewCount')
                    qViewCount.text = questionViewCount
                    
                    uncleanData = ET.tostring(root,encoding='utf-8')
                    cleanData, numberReplaced = re.subn("&#10|&#xA|\\n|\\r", " ", uncleanData)
                    
                if validQuestion:
                    newXmlFile.write(cleanData)
                    newXmlFile.write('\n')
                    questionCounter += 1
                    validQuestion = False
                
                if questionCounter == MAX_QUESTIONS:
                    newXmlFile.write('</questions>')
                    newXmlFile.close()
                    print ' File number ' + str(fileNumber) + ' done!!!'
                    questionCounter = 0
                    fileNumber += 1
                    newXmlFile = open(os.path.join(Path,NewXmlFileNamePrefix+str(fileNumber)+NewXmlFileNameExtension),'wb')
                    newXmlFile.write('<questions>\n')
    newXmlFile.write('</questions>')
    newXmlFile.close()
    print ' File number ' + str(fileNumber) + ' done!!!'
    