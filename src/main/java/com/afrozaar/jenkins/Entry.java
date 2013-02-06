package com.afrozaar.jenkins;

import org.kohsuke.stapler.DataBoundConstructor;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;

public class Entry extends AbstractDescribableImpl<Entry> {

    private final String storageAccount;

    private final String sourcePath;

    private final String destinationPath;

    private final String container;

    @DataBoundConstructor
    public Entry(String storageAccount, String sourcePath, String destinationPath, String container) {
        super();
        this.storageAccount = storageAccount;
        this.sourcePath = sourcePath;
        this.destinationPath = destinationPath;
        this.container = container;
    }

    public String getStorageAccount() {
        return storageAccount;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public String getContainer() {
        return container;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Entry> {
        public String getDisplayName() {
            return "Entry";
        }
    }

}
