# Web Phishing Monitoring Framework

## About the Project

Java based open source application that uses selenium webdriver, headless firefox and browsermob-proxy to track content and metadata from phishing websites.

## Build

This project is built using Maven 3 and Java 11.
To build it, in the root directory of this repository run:
```sh
mvn package
```

It will create the directory `target`, containing the file
`WebPhishingFramework.jar` and the sub directory `lib` with all the
dependencies that are automatically installed by Maven.

## Run
This project depends on Selenium `3.141.59`.
Also, since the web page inspection uses the Firefox Web Browser with
Geckodriver, ensure that both of these two dependencies are installed
in the proper compatible versions.
To check the compatibility list, please visit
[Geckdriver Support](https://firefox-source-docs.mozilla.org/testing/geckodriver/Support.html).

After installing Firefox and Geckdriver, build the project as shown in
[Build](#build).

Then, from the root directory, the application can be run as:
```sh
java -jar target/WebPhishingFramework.jar path_to_config_file.json
```

Notice that JSON file must be passed as an execution argument.
This is supposed to have all the required execution parameters to run the
application.
We've preserved a configuration environment in the folder
[example](example).
So, to run the example configuration environment, call the application
as follows:
```sh
java -jar target/WebPhishingFramework.jar ./example/config.json
```

To understand the configuration environment, go to
[Execution Environment](#execution-environment).


### Using the docker file

For that option, we advise to use the same structure of
the [example](example) directory.
It means, using a single directory containing all the needed
parameters and files, including the configuration.
Notice that our Docker image already contains the
**Geckodriver** binary at `/usr/bin/geckodriver`.
Thus, from the root directory of this repository, the
framework can be run as:

```
docker run -it -v `pwd`/example:/root/environment vrjuliao/web-phishing-framework:latest
```


## Execution Environment

### Configuration file
It is a JSON file containing an object with five parameters:

* `concurrentBrowsers`: **\[REQUIRED\]** Number of concurrent browser instances.
* `pageTimeout`: **\[REQUIRED\]** Page timeout.
* `windowTimeout`: **\[REQUIRED\]** Time window for request limiting.
* `maxRequests`: **\[REQUIRED\]** Request limit per defined time window.
* `repositoryPath`: **\[REQUIRED\]** The path of the file or folder containing
  the list of urls to be monitored.
  If it is a file, the urls must be separated by a line breaker.
  Otherwise, if it is a directory, all the files into that must have only urls
  separated by line breakers.
* `geckodriverBinPath`: **\[REQUIRED\]** The path of the Geckodriver binary
  file.
* `runtimeControllersPath`: **\[REQUIRED\]** The path of the directory which
  contains all the [Runtime Control Files](#remote-controller-files).
* `logsDirPath`:  The directory where all the output logs of this application
  will be written.
  **WARNING**: This is not a required configuration, but if it is not provided,
  the current work directory is chosen as the logs destination.
* `whiteListPath`: The path of the file or folder containing the list of urls
  of the white list. <!--TODO: improve the explanation about the white list -->
  If it is a file, the urls must be separated by a line breaker.
  Otherwise, if it is a directory, all the files into that must have only urls
  separated by line breakers.
* `blackListPath`: The path of the file or folder containing the list of urls
  of the black list. <!--TODO: improve the explanation about the black list -->
  If it is a file, the urls must be separated by a line breaker.
  Otherwise, if it is a directory, all the files into that must have only urls
  separated by line breakers.

> All the configurations with the suffix `Path` can be specified by absolute
paths or relative paths from the same directory of the configuration file.

See the example configuration file at [config.json](example/config.json).

### Runtime Control Files
Our goal is to provide a monitor that can be incremented while it keeps
running.
For that task, we've used file readers to capture the commands that are
passed in runtime.
Such files must be placed into the directory specified by the
`runtimeControllersPath` configuration.

* `running`: a text file named `running` that handles only one char: either `0`
  or `1`.
  When the monitor is started, such property must be started with `1`.
  In case the user needs to stop the application, just override the value by
  `0`.
  It makes able to kill the monitor by a simple command like:
  ```sh
  echo 0 > <path_to_runtimeControllersPath>/running
  ```

### Log directories


* `time_urls`: contains four timestamps  (in milliseconds) containing the respective information:
  - Start time of the process for one URL.
  - Start time of the page download.
  - End time of the page download.
  - End time of the page processing.

## License
Distributed under MIT license.
