# UrMoAC
A tool for computing accessibility measures, supporting aggregation, variable limits, and intermodality developed at the Institute of Transport Research at the German Aerospace Center (DLR).

The tool is licensed under the [Eclipse Public License 2.0](LICENSE.md).

When using it, you are obligated to cite it as:

Daniel Krajzewicz, Dirk Heinrichs and Rita Cyganski (2017) [_Intermodal Contour Accessibility Measures Computation Using the 'UrMo Accessibility Computer'_](https://elib.dlr.de/118235/). International Journal On Advances in Systems and Measurements, 10 (3&4), Seiten 111-123. IARIA.

Please visit the <a href="https://github.com/DLR-VF/UrMoAC/wiki/index">wiki</a> for further information.

## Build

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