# DITAG-Tool

DITAG-Tool (Data Interoperability Translators Automatic Generator Tool) 
is a tool that verifies semantic and data compatibility between heterogeneous systems 
and automatically generates translators when compatible, 
using schemas semantically annotated against a reference ontology.

## Source Code

The source code of DITAG-Tool is publicly available in this repository.

## Project

DITAG-Tool was developed in the context of the Arrowhead fPVN project (https://doi.org/10.3030/101111977)

## Contributors

- Afonso Fernandes-Oliveira
- Filipe Moutinho
- João Rosas
- Carolina Lagartinho-Oliveira
- Pedro Maló

## How to Run DITAG-Tool to Generate a Translator
 
```cmd
@echo off

REM Execute the ditag-tool.jar with the corresponding files
REM
REM NOTE: Change --providerData, --consumerData and --ontologyData below
REM       to point to the files for the scenario you want to run.
REM
REM NOTE: --reasoner can be changed between "elk" and "pellet"
REM       depending on the reasoner you want to use.
REM       To use the Pellet reasoner, uncomment the corresponding dependency in the `pom.xml` file and the related code in the Java source files.
REM
REM NOTE: --logLevel:
REM       0 = off, 1 = info, 2 = debug

java -jar ./ditag-tool.jar ^
--providerData="./P1.xsd" ^
--providerType=file ^
--consumerData="./C1.xsd" ^
--consumerType=file ^
--ontologyData="./ontology.owl" ^
--ontologyType=file ^
--outType=file ^
--outDirectory=. ^
--matchType=first ^
--logLevel="0" ^
--reasoner="elk"
```

### How to Run the Generated Translator
 
```cmd
REM Compile the translator class, using ditag-tool.jar as classpath
javac -d . -cp ./ditag-tool.jar Translator.java

REM Run the translator: this is the provider message.
REM NOTE: change "./P1.xml" to the provider file you want to translate.
java -cp ./ditag-tool.jar;. pt.uninova.ditag.translator.Translator . file "./P1.xml"
```
