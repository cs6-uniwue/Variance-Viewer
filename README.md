![viewer](https://user-images.githubusercontent.com/23743591/45493637-8840c880-b76f-11e8-8efc-4e92d986aea6.png)

# Variance-Viewer

Variance-Viewer is a automatic open-source tool for text comparison with an extendable rule based variance analysis.
Documents are compared on word basis, while their variance is displayed on character basis.

*Plain text* documents, as well as *TEI* documents are supported as input, while *pdf*, *TEI* and *json* are supported as an output format.
The json output format is simple and can for example be examined further with the semantic analysis tool [webAthen](http://webathen.informatik.uni-wuerzburg.de/)

Please feel free to test the Variance-Viewer for yourself with the official [web application](http://variance-viewer.informatik.uni-wuerzburg.de/).

This tool has been created in a project of the [Chair of Computer Science VI - Artificial Intelligence and Applied Computer Science of the University of Würzburg](http://www.is.informatik.uni-wuerzburg.de/en/homepage/), in the working group of [Prof. Dr. Frank Puppe](http://www.is.informatik.uni-wuerzburg.de/staff/puppe-frank/).


## Table of Contents
- [Installing](#installing)
  * [Linux](#linux)
  * [Windows](#windows)
  * [Mac OS X](#mac-os-x)
- [Running](#running)
- [Configuration](#configuration)

## Installing

### Linux
For this guide tomcat version 8 and Ubuntu is used.

#### Packages
`apt-get install tomcat8 maven openjdk-8-jdk`

#### Clone Repository
`git clone https://github.com/NesbiDevelopment/Variance-Viewer.git` 

#### Compile
run `mvn clean install -f Variance-Viewer/pom.xml`.

#### Copy or link the created war file to tomcat
Either: `sudo ln -s $PWD/Variance-Viewer/target/Variance-Viewer.war /var/lib/tomcat8/webapps/Variance-Viewer.war`

or `cp Variance-Viewer/target/Variance-Viewer.war /var/lib/tomcat8/webapps/Variance-Viewer.war`

#### Start Tomcat
`systemctl start tomcat8`

to restart `systemctl restart tomcat8`

to start automatically at system boot `systemctl enable tomcat8`

### Windows
It is recommended to use Eclipse.

#### Java EE for Web Developer
In Eclipse go to `Help -> Install New Software` and select `All sources` under `Work with`.

Afterwards select and install `Web, XML, Java EE and OSGi Enterprise Development`

#### Apache Tomcat
Download and extract the most recent version under http://tomcat.apache.org/download-90.cgi.

Open `Window` -> `Show View` -> `Other...` -> `Server` -> `Servers`

Click the info that promts to add a new server, select `Apache` -> `Tomcat <version> Server` -> `Next`-> set your Tomcat installation directory -> `Finish`.

#### Import Project
Open `File` -> `Import` and select `Maven` -> `Existing Maven Projects` -> `Next`.

Set the projects directory as the root directory and select `Finish`.

If the maven project has not updated automatically, rightclick on `Variance-Viewer` -> `Maven` -> `Update Project...` -> `OK`

#### Start Tomcat
Rightclick on `Variance-Viewer` -> `Run As` -> `Run on Server`.


### Mac OS X

#### Homebrew
Install homebrew (see https://brew.sh/).

Afterwards install all required packages (java, Tomcat, git, and maven):

`brew cask install java`

`brew install tomcat git maven`

To verify the Tomcat installation use homebrew’s services utility. 

Tomcat should now be listed in the following command:

`brew services list`

#### Clone Repository
`git clone https://github.com/NesbiDevelopment/Variance-Viewer.git` 

#### Compile
run `mvn clean install -f Variance-Viewer/pom.xml`.

#### Copy or link the created war file to tomcat
Either: `sudo ln -s $PWD/Variance-Viewer/target/Variance-Viewer.war /usr/local/Cellar/tomcat/[version]/libexec/webapps/Variance-Viewer.war`

or `cp Variance-Viewer/target/Variance-Viewer.war /usr/local/Cellar/tomcat/[version]/libexec/webapps/Variance-Viewer.war`

#### Start Tomcat
`brew services start tomcat`

to restart `brew services restart tomcat`


## Running
### Access in browser
Go to `localhost:8080/Variance-Viewer`.


## Configuration ##
Variance-Viewer contains a default configuration file (src/webapp/WEB-INF/default.txt) with a few settings that can be set before running the application. 
A user can also provide a configutarion file by adding it at the home menu, with a typical settings file beeing as follows:

```
:css:
.wide{letter-spacing: 3px;}
.sizeXL{font-size: 110%;}
.sizeXXL{font-size: 130%;}
:css:


:punctuations:
,;/\\-\\.\\?!—–´
:punctuations:

:graphemes:
y i
c t
u v
ſ s
ſs ss
ß ss
å ao
& et
ce ze
ci zi
co ko
ca ka
cu ku
ä ae
ö oe
ü ue
:graphemes:

:abbreviations:
Dr. doctor
Prof. professor
:abbreviations:

:contenttags:
head p
:contenttags:
``` 

### :css:
The css tag allows the user to specify the visual appearance of the viewer.
A specific font for example or a visual appearance for TEI render attributes (rend) can be set here.
Render types are named equally as in the TEI documents specified and an example for a *wide* rend class could be as follows:

```
:css:
.wide{
	letter-spacing: 3px;
}
:css:
```

### :punctuations:
Punctuations to consider in the punctuation variance class can be specified in the setting :punctuations:.
These symbols can be listed in sequence as follows:

```
:punctuations:
,;/-.?!—–
:punctuations:
```

### :graphemes:
Graphemic changes in words like *ö* to *oe* or *å* to *ao* can be set in the :graphemes: setting.
Every line describes one graphemic change and the list is processed from top to bottom and left to right.
This means that the tokens *maß*, *maſs* and *mass* for example are, with the following rules, considered graphemic changes to one another. 

```
:graphemes:
ſs ss
ß ss
:graphemes:
```

### :abbreviations:
A abbreviation like *Dr.* for *doctor* can be specified in the :abbreviations: setting.
These rules for abbreviation changes are build in the same way as rules for graphemic changes.

### :contenttags:
This tool supports the comparison of TEI documents.
Text between tags, that are specified here, is tokenized and compared.
The following setting will compare every text between head and p tags between two TEI documents: 

```
:contenttags:
head p
:contenttags:
```
