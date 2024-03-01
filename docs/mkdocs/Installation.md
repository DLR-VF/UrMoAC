These installation instructions are suitable for windows and are tested under Windows 10 and [Java](https://projects.eclipse.org/projects/adoptium.temurin) jdk-17.0.9+9. Ensure that a Java Development Kit (at least version 1.8) is installed and that the folder containing the java.exe file is in your path environment variable.

## Download the latest release
The easiest way is to download the latest release. It should contain everything needed to run the software.

As UrMoAC is a Java application, the most simple way to obtain a valid version is to download the latest release from [https://github.com/DLR-VF/UrMoAC/releases](https://github.com/DLR-VF/UrMoAC/releases).


## Install using Maven
### Step 1: Clone the UrMoAC repository
Be sure that you have installed git and download the source code using 

    git clone https://github.com/DLR-VF/UrMoAC.git

on the command line in a folder of your choice.

### Step 2: Set-up Maven
An executable jar file can be built with maven. Please install maven (https://maven.apache.org/install.html) on your computer, and be sure that you include JAVA_HOME and M2_HOME in your environment variables.
Add the maven/bin folder to your path (https://en.wikipedia.org/wiki/PATH_(variable) ) variable.

### Step 3: Build using Maven
Open the command line. Go to the directory you just cloned. Type:

    mvn package

et voila. All dependencies will be downloaded and there will be a new folder called target which includes the executable UrMoAccessibilityComputer-[VERSION]-SNAPSHOT-executable.jar.



## Shaded JAR using Maven

You can build a shaded JAR file to execute UrMoAC. 
UrMoAC contains the external library optionslib (see libs directory). 

To build the JAR file correctly you need to add the optionslib to your local Maven repositories using the following command:

```shell
mvn install:install-file -Dfile=libs/optionslib-1.2.jar -DgroupId=de.dks.utils.options -DartifactId=optionslib -Dversion=1.2 -Dpackaging=jar        
```

After that, you can build the final JAR using:
```shell
mvn clean package
```

After finishing the build process, you will see a target directory in your project directory that contains the shaded JAR file.


## Eclipse import

Install m2e plugin for eclipse.
Be sure that you have installed git and
download the source code using &ldquo;git clone https://github.com/DLR-VF/UrMoAC.git&rdquo; on the command line in a folder of your choice.

Open Eclipse and use File -> import. Click on Maven -> existing Maven Projects -> next

The root directory on the next page should be your cloned folder. Select the pom.xml under projects and click on finish.

Now you should have an eclipse Maven project where you can run and debug the java code.

## JetBrains IntelliJ import

### Start a new project with VCS
Start a new project with the latest version of UrMoAC via the &ldquo;get from VCS&rdquo; option in IntelliJ. Select **Git** as version control and paste the following URL, if you are using SSH: 
`git@github.com:DLR-VF/UrMoAC.git`.
Then continue with next. 

You may have to enter your SSH-key passphrase now. Alternatively, you can clone the project via HTTPS: [https://github.com/DLR-VF/UrMoAC.git](https://github.com/DLR-VF/UrMoAC.git).

After that, the most recent version is downloaded and opened as a new project in IntelliJ. 

### Build using Maven

In the new project, select File -> Project Structure. Under the section &ldquo;Project settings&rdquo; select your Project SDK. The recommended version is Java 8 (sometimes also referred to by the version name 1.8.0).

To build the project using Maven, select View -> Tool Windows -> Maven. A new tool window should appear on the right side of your IDE. In the dropdown menu, select your project name -> Lifecycle. There, select `package` by double-clicking to build the project. This yields the same results as in Step 3 of &ldquo;Install using Maven&rdquo;.