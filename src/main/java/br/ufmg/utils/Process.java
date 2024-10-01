package br.ufmg.utils;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.littleshoot.proxy.HttpFilters;
import org.littleshoot.proxy.HttpFiltersAdapter;
import org.littleshoot.proxy.HttpFiltersSourceAdapter;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.remote.DesiredCapabilities;

import com.google.common.net.HttpHeaders;

import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import net.lightbody.bmp.BrowserMobProxy;
import net.lightbody.bmp.BrowserMobProxyServer;
import net.lightbody.bmp.client.ClientUtil;
import net.lightbody.bmp.core.har.HarEntry;

import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.apache.commons.codec.digest.DigestUtils;
import org.openqa.selenium.firefox.FirefoxDriverLogLevel;

public class Process implements Runnable {


    /* 
        Vazamento de memória - biblioteca que faz o dump das classes estão gastando um certo percentual
        de memória
        
        conseguimos mapear qual objeto está ocupando muita memória
        quanto de memória gasta cada objeto

        aumentar o tamanho da RAM


    */
	private int pid;
	private int timeout;
	private int requestsLimit;
	private BrowserMobProxy proxy;
	private Proxy seleniumProxy;
	private FirefoxDriver driver;
	private final BlockingQueue<String> listaUrls;
	private AtomicBoolean killProcesses;
	private AtomicBoolean restartProcesses;
	private Map<String, Integer> blockedDomains;
	private LogsWriter logsWriter;
	private URLList whitelist;
	private URLList blacklist;
	private final String geckoDriverBinaryPath;
  	private static final Logger LOGGER = LogManager.getLogger();

	public Process(BlockingQueue<String> urlsList,
			AtomicBoolean killProcesses,
			AtomicBoolean restartProcesses,
			int id,
			LogsWriter logsWriter,
			URLList whitelist,
			URLList blacklist,
			int timeout,
			int requestsLimit,
			String geckoDriverBinaryPath) {

		this.timeout = timeout;
		this.whitelist = whitelist;
		this.blacklist = blacklist;
		this.listaUrls = urlsList;
		pid = id;
		this.logsWriter = logsWriter;
		blockedDomains = new HashMap<String, Integer>();
		this.killProcesses = killProcesses;
		this.restartProcesses = restartProcesses;
		this.requestsLimit = requestsLimit;
		this.geckoDriverBinaryPath = geckoDriverBinaryPath;
	}

        public void getProxyServer() {
            proxy = new BrowserMobProxyServer();

            proxy.addRequestFilter((request, contents, messageInfo) -> {

                String urlReq = io.netty.handler.codec.http.HttpHeaders.getHost(request);
                String dom = "";
                dom = urlReq.split(":")[0];

                request.headers().set("X-Research-Project-Info", "http://138.197.3.28/");

                if (!dom.contains("firefox") && !dom.contains("mozilla") && !dom.contains("proxy")) {
                    long tempo = System.currentTimeMillis();
                    if (Singleton.getInstance().isInDict(dom)) {
                        int numRequisicoes = Singleton.getInstance().getNumeroReq(dom);
                        LOGGER.info(dom + " " + numRequisicoes);
                        Singleton.getInstance().setRequestsNumber(dom, tempo);
                        if (numRequisicoes >= requestsLimit && !whitelist.has(dom)) {
                            final HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(),
                                    HttpResponseStatus.valueOf(405));
                            response.headers().add(HttpHeaders.CONNECTION, "Close");
                            return response;
                        }
                    } else {
                        Singleton.getInstance().setRequestsNumber(urlReq, tempo);
                    }
                }

                if (request.getMethod().equals(HttpMethod.POST) || dom.contains(".gov") || blacklist.has(dom)) {
                    LOGGER.info("Headers: " + request.headers() + "\n");
                    final HttpResponse response = new DefaultHttpResponse(request.getProtocolVersion(),
                            HttpResponseStatus.valueOf(405));
                    response.headers().add(HttpHeaders.CONNECTION, "Close");
                    return response;
                } else {
                    return null;
                }
            });

            // ---------------------------------------@---------------------------------
            proxy.addLastHttpFilterFactory(new HttpFiltersSourceAdapter() {
                @Override
                public HttpFilters filterRequest(HttpRequest originalRequest) {
                    return new HttpFiltersAdapter(originalRequest) {
                        @Override
                        public HttpResponse proxyToServerRequest(HttpObject httpObject) {
                            if (httpObject instanceof HttpRequest) {
                                ((HttpRequest) httpObject).headers().remove("VIA");
                                LOGGER.info(((HttpRequest) httpObject).headers().get("VIA"));
                            }
                            return null;
                        }
                    };
                }
            });

            proxy.setTrustAllServers(true);
            proxy.start(0);
        }

	public void getSeleniumProxy() {
		seleniumProxy = ClientUtil.createSeleniumProxy(proxy);
		try {
			String hostIp = Inet4Address.getLocalHost().getHostAddress();
			seleniumProxy.setHttpProxy(hostIp + ":" + proxy.getPort());
			seleniumProxy.setSslProxy(hostIp + ":" + proxy.getPort());
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
	}

	public void getFirefoxDriver(DesiredCapabilities capabilities) {
		FirefoxOptions options = new FirefoxOptions();
		options.setProxy(seleniumProxy);
        options.addArguments("--headless");
        options.setBinary("/usr/bin/firefox");
		options.merge(capabilities);
		driver = new FirefoxDriver(options);
	}

	public Response accessURL(String composedURL) {
		String[] temp = composedURL.split("  ");
		String url = temp[0];
		LOGGER.info(url);

		String dom = "";
		if (url.contains("http") == true) {
			dom = url.split("/")[2];
		} else {
			dom = url.split("/")[0];
		}

		if (blockedDomains.get(dom) != null && blockedDomains.get(dom) >= 10) {
			String out = composedURL.replace("\n", "") + "  BLOCKED  0\n";
			Response response = new Response(true, false, out);
			return response;
		}

		proxy.newHar("url_" + Integer.toString(pid));
		driver.manage().timeouts().pageLoadTimeout(this.timeout, TimeUnit.SECONDS);
		String finalUrl = "about:blank";
		try {
			this.logsWriter.writeTimeURLs(this.pid, "ACESSO A URL: " + url + Long.toString(System.currentTimeMillis()) + " ");
			driver.get(url);
			this.logsWriter.writeTimeURLs(this.pid, " FINALIZANDO DRIVER.GET DA URL" + Long.toString(System.currentTimeMillis()) + " \n");
			finalUrl = driver.getCurrentUrl();
		} catch (Exception e) {
			if (e instanceof WebDriverException) {
				if (blockedDomains.get(dom) == null) {
					blockedDomains.put(dom, 1);
				} else {
					int valor = blockedDomains.get(dom);
					valor += 1;
					blockedDomains.replace(dom, valor);
				}
			}
			String urlBroken = composedURL;

			try {
				logsWriter.writeFirefoxException(this.pid, urlBroken + e.toString());
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			String executionName = e.getClass().getSimpleName();
			String out = composedURL.replace("\n", "") + "  " + executionName + "  0\n";
			Response response = new Response(true, false, out);
			return response;
		}

		if (finalUrl != "about:blank") {

			InetAddress ip = null;
			String ipString = null;
			try {
				String hostname = new URL(finalUrl).getHost();
				ip = InetAddress.getByName(hostname);
				ipString = ip.getHostAddress();
			} catch (MalformedURLException e) {
				// e.printStackTrace();
				finalUrl = "-";
				ipString = "0";
			} catch (UnknownHostException e) {
				// e.printStackTrace();
				if (blockedDomains.get(dom) == null) {
					blockedDomains.put(dom, 1);
				} else {
					int valor = blockedDomains.get(dom);
					valor += 1;
					blockedDomains.replace(dom, valor);
				}
				finalUrl = "-";
				ipString = "0";
			}

			String out = composedURL.replace("\n", "") + "  " + finalUrl + "  " + ipString + "\n";
			// TODO: tem uma exceção sendo lançada abaixo: (consertada , talvez)
			String hash;
			String page;
			try {
				String html = driver.getPageSource();
				Document document = Jsoup.parse(html);
				page = document.toString();

				hash = DigestUtils.md5Hex(page);
			} catch (Exception e) {
				page = "";
				hash = "EMPTYPAGE";
			}

			String url8 = out.replace("\n", "") + "  " + hash.toString() + "\n";

			try {
				this.logsWriter.writeSourcePage(this.pid, url8);
				this.logsWriter.writeSourcePage(this.pid, page);
				this.logsWriter.writeSourcePage(this.pid, "\n*!-@x!x@-!*\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			LOGGER.info("finished");
			return new Response(false, false, out, proxy.getHar().getLog().getEntries());

		}
		return new Response(true, false, "wtf");
	}

	public void run() {
		System.setProperty("webdriver.gecko.driver", this.geckoDriverBinaryPath);

		DesiredCapabilities capabilities = new DesiredCapabilities();

		getProxyServer();
		getSeleniumProxy();
		capabilities.setCapability("marionette", true);
		getFirefoxDriver(capabilities);

		// abrirArquivos();

		while (killProcesses.get() == false) {
			try {
				if (restartProcesses.get()) {
					break;
				}
				long startTime = System.currentTimeMillis();
				String composedURL = listaUrls.take();
                LOGGER.info("PID: " + this.pid + "\n");
                this.logsWriter.writeTimeURLs(this.pid, "\nSTART TIME PEGAR COMPOSED URL: " + composedURL + Long.toString(startTime) + " ");

				if (composedURL == "poison_pill") {
					killProcesses.compareAndSet(false, true);
                    this.logsWriter.writeTimeURLs(this.pid, "ComposedURL == Poison_Pill");

					break;
				}

				Response response = accessURL(composedURL);
				String urlLog = response.getUrlLog();
				this.logsWriter.writeAccessLog(this.pid, urlLog);
				this.logsWriter.writeTcp(this.pid, urlLog.replace("\n", ""));
				this.logsWriter.writeCadeiaURLs(this.pid, urlLog);

				Set<String> ipsSet = new HashSet<String>();
				if (response.getException() == false && response.getBlocked() != true) {
					List<HarEntry> entries = response.getHar();
					for (HarEntry entry : entries) {
						String ip = entry.getServerIPAddress();
						int statusCode = entry.getResponse().getStatus();
						LOGGER.info(statusCode);
						ipsSet.add(ip);
						String initialURLString = entry.getRequest().getUrl();
						String finalURLString = entry.getResponse().getRedirectURL();

						if (!finalURLString.contains("mozilla") && !initialURLString.contains("mozilla")
								&& !finalURLString.contains("firefox") && !initialURLString.contains("firefox")) {
							if (finalURLString != "") {
								String timeStamp = entry.getStartedDateTime().toString();
								this.logsWriter.writeCadeiaURLs(this.pid, timeStamp.replace(" ", "") + "  "
										+ initialURLString + "  " + finalURLString + "  " + statusCode);
							} else {
								if (initialURLString != "") {
									String timeStamp = entry.getStartedDateTime().toString();
									this.logsWriter.writeCadeiaURLs(this.pid, timeStamp.replace(" ", "") + "  "
											+ initialURLString + "  -" + statusCode + "\n");
								}
							}
						}
					}
					String ipsChain = String.join(",", ipsSet);

					this.logsWriter.writeTcp(this.pid, "  " + ipsChain);
				}

				this.logsWriter.writeTcp(this.pid, "\n*!-@x!x@-!*\n");
				this.logsWriter.writeCadeiaURLs(this.pid, "*!-@x!x@-!*\n");
				long finalTime = System.currentTimeMillis();
				this.logsWriter.writeTimeURLs(this.pid, " Finalizando acesso composed url " + Long.toString(finalTime) + "\n");

			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			driver.quit();
			proxy.stop();
		} catch (Exception e) {

		}

	}
}