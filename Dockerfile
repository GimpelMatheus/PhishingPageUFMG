FROM selenium/standalone-firefox:106.0-geckodriver-0.32 as selenium
RUN chmod +x /var/run/docker.sock
RUN adduser --uid 1015 --gid 1015 mgimpel
RUN adduser mgimpel docker
USER mgimpel
ENV HOME /home/mgimpel
ENV WEB_PHISHING_CONFIG_FILE $HOME/PhishingPageProject/web-phishing-framework/example/config.json
ENV WEB_PHISHING_JAR $HOME/PhishingPageProject/web-phishing-framework/target/WebPhishingFramework.jar

RUN mkdir -p $HOME/workdir
WORKDIR $HOME/workdir

COPY target/WebPhishingFramework.jar $HOME/workdir/
COPY target/lib $HOME/workdir/lib
RUN chown -R mgimpel:mgimpel $HOME/workdir

ENTRYPOINT java -jar $WEB_PHISHING_JAR $WEB_PHISHING_CONFIG_FILE
