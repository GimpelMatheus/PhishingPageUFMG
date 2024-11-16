FROM selenium/standalone-firefox:106.0-geckodriver-0.32 as selenium
USER root
ENV HOME /root
ENV WEB_PHISHING_CONFIG_FILE $HOME/environment/config.json
ENV WEB_PHISHING_JAR $HOME/workdir/WebPhishingFramework.jar

RUN mkdir -p $HOME/workdir
WORKDIR $HOME/workdir

COPY target/WebPhishingFramework.jar $HOME/workdir/
COPY target/lib $HOME/workdir/lib

ENTRYPOINT java -jar $WEB_PHISHING_JAR $WEB_PHISHING_CONFIG_FILE
