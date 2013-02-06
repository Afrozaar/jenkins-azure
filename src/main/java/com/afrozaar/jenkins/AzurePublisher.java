package com.afrozaar.jenkins;

import com.microsoft.windowsazure.services.blob.client.BlobOutputStream;

import com.microsoft.windowsazure.services.blob.client.CloudBlobClient;
import com.microsoft.windowsazure.services.blob.client.CloudBlobContainer;
import com.microsoft.windowsazure.services.blob.client.CloudBlockBlob;
import com.microsoft.windowsazure.services.core.storage.CloudStorageAccount;

import org.apache.commons.io.IOUtils;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import net.sf.json.JSONObject;

import hudson.Extension;
import hudson.FilePath;
import hudson.FilePath.FileCallable;
import hudson.Launcher;
import hudson.model.BuildListener;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.remoting.VirtualChannel;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;

import javax.servlet.ServletException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class AzurePublisher extends Notifier implements Serializable {

    private List<Entry> azurePublishInstances;

    @DataBoundConstructor
    public AzurePublisher(List<Entry> azurePublishInstances) {
        super();
        this.azurePublishInstances = azurePublishInstances != null ? azurePublishInstances : new ArrayList<Entry>();
    }

    // Overridden for better type safety.
    // If your plugin doesn't really define any property on Descriptor,
    // you don't have to do this.
    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, final BuildListener listener) throws InterruptedException,
            IOException {
        listener.getLogger().println("Hello... this is the azure build publisher running");

        boolean x = true;
        for (Entry instance : azurePublishInstances) {
            x = x && deploy(build, listener, instance);
        }
        return x;
    }

    private boolean deploy(AbstractBuild<?, ?> build, final BuildListener listener, final Entry instance) throws IOException,
            InterruptedException {
        listener.getLogger().println("storage account: " + instance.getStorageAccount());
        listener.getLogger().println("container: " + instance.getContainer());
        listener.getLogger().println("source: " + instance.getSourcePath());
        listener.getLogger().println("destination: " + instance.getDestinationPath());

        final String source = build.getEnvironment(listener).expand(instance.getSourcePath());
        final String destination = build.getEnvironment(listener).expand(instance.getDestinationPath());

        listener.getLogger().println("replaced: " + source);
        listener.getLogger().println("replaced: " + destination);
        FilePath workspace = build.getWorkspace();
        FilePath child = workspace.child(source);
        listener.getLogger().println("childpath:" + child);

        /*
         * // make 'file' a fresh empty directory. file.act(new
         * FileCallable<Void>() { // if 'file' is on a different node, this
         * FileCallable will // be transfered to that node and executed there.
         * public Void invoke(File f,VirtualChannel channel) { // f and file
         * represents the same thing f.deleteContents(); f.mkdirs(); } });
         */
        // file path may not be on local file system so need to execute it where
        // it is
        return child.act(new FileCallable<Boolean>() {

            public Boolean invoke(final File f, VirtualChannel channel) throws IOException, InterruptedException {
                try {
                    return deployFileToBlobStorage(instance.getStorageAccount(), instance.getContainer(), f, destination,
                            listener.getLogger());
                } catch (Exception e) {
                    throw new IOException("problem deploying to blob storage", e);
                }
            }
        });
    }

    @Extension
    // This indicates to Jenkins that this is an implementation of an extension
    // point.
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> implements Serializable {

        /**
         * Performs on-the-fly validation of the form field 'name'.
         * 
         * @param value
         *            This parameter receives the value that the user has typed.
         * @return Indicates the outcome of the validation. This is sent to the
         *         browser.
         */
        public FormValidation doCheckStorageAccount(@QueryParameter String value) throws IOException, ServletException {
            if (value.length() == 0)
                return FormValidation.error("Please set a storage account");
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            // Indicates that this builder can be used with all kinds of project
            // types
            return true;
        }

        /**
         * This human readable name is used in the configuration screen.
         */
        public String getDisplayName() {
            return "Deploy to Azure Blob Storage";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {

            save();
            return super.configure(req, formData);
        }
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    public boolean deployFileToBlobStorage(String storageAccount, String containerName, File file, String destinationName,
            PrintStream logger) throws Exception {

        logger.println("=======");
        logger.println("starting");
        logger.println("StorageAccount=" + storageAccount);
        logger.println("deploy file  = " + file);

        logger.println("File:" + file);

        CloudStorageAccount object = CloudStorageAccount.parse(storageAccount);
        CloudBlobClient blobClient = object.createCloudBlobClient();
        CloudBlobContainer containerReference = blobClient.getContainerReference(containerName);
        containerReference.createIfNotExist();

        if (destinationName == null || "".equals(destinationName)) {
            destinationName = file.getName();
        }
        logger.println("Creating blob in container :" + containerReference.getUri() + " called " + destinationName);
        CloudBlockBlob blockBlobReference = containerReference.getBlockBlobReference(destinationName);
        logger.println("deploying " + file + " to " + blockBlobReference.getUri());
        BlobOutputStream openOutputStream = blockBlobReference.openOutputStream();
        FileInputStream input = new FileInputStream(file);
        IOUtils.copy(input, openOutputStream);
        openOutputStream.close();
        input.close();
        logger.println("DONE");
        logger.println("=======");
        return true;

    }

    public List<Entry> getAzurePublishInstances() {
        return azurePublishInstances;
    }

    public void setAzurePublishInstances(List<Entry> instances) {
        this.azurePublishInstances = instances;
    }
}
