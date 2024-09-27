package br.ufmg.utils;

import java.util.List;

import net.lightbody.bmp.core.har.HarEntry;

public class Response {

    private boolean blocked;
    private boolean exception;
    private boolean processed;
    private String urlLog;
    private List<HarEntry> entries;

    // Constructor for blocked or exception responses
    public Response(boolean blocked, boolean exception, String urlLog) {
        this.blocked = blocked;
        this.exception = exception;
        this.processed = false; // Not processed by default
        this.urlLog = urlLog;
    }

    // Constructor for processed responses with HAR entries
    public Response(boolean blocked, boolean exception, String urlLog, List<HarEntry> entries) {
        this.blocked = blocked;
        this.exception = exception;
        this.processed = true; // Mark as processed
        this.urlLog = urlLog;
        this.entries = entries;
    }

    public Boolean getBlocked() {
		return blocked;
	}

	public Boolean getException() {
		return exception;
	}

	public String getUrlLog() {
		return urlLog;
	}

	public List<HarEntry> getHar() {
		return entries;
	}

    // New methods

    // Check if the response was processed
    public boolean getProcessed() {
        return processed;
    }

    // Get the HAR entries if the response was processed, otherwise return null
    public List<HarEntry> getResult() {
        return processed ? entries : null;
    }
}
