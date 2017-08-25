#! /usr/bin/python3
#coding:utf-8
import re

'''自动给testcase节点生成序列号'''
def addNo(inputFile, outputFile):
    try:
        f = open(inputFile, mode='r', encoding='utf-8')
        f1 = open(outputFile, mode='w', encoding='utf-8')
        ino = 0
        for line in f:
            if line.find('<TestCase') == -1:
                f1.write(line)
            else:
                ino = ino + 1
                sno = '%05d' % ino
                replaceLine = '<TestCase no="%s" ' % sno
                if(line.find('<TestCase no=')) != -1:
                    newLine = re.sub('<TestCase no=\"(.*?)\" ', replaceLine, line)
                    f1.write(newLine)
                else:
                    newLine = line.replace('<TestCase', replaceLine)
                    f1.write(newLine)
    except Exception as e:
        print('parse file failed')
    finally:
        if f is not None:
            f.close()
        if f1 is not None:
            f1.flush()
            f1.close()

if __name__ == '__main__':
    inputFile = 'TestPlan.xml'
    outputFile = 'TestPlan_new.xml';
    addNo(inputFile, outputFile)
