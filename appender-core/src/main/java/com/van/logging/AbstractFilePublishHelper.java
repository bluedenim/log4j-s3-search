package com.van.logging;

import java.io.*;
import java.util.Objects;
import java.util.zip.GZIPOutputStream;

public abstract class AbstractFilePublishHelper implements IPublishHelper<Event> {

    private final boolean compressEnabled;
    protected final boolean verbose;

    private File tempFile;
    private Writer outputWriter;

    public AbstractFilePublishHelper(boolean compressEnabled, boolean verbose) {
        this.compressEnabled = compressEnabled;
        this.verbose = verbose;
    }

    @Override
    public void start(PublishContext context) {
        try {
            tempFile = File.createTempFile("toBePublished", null);
            OutputStream os = createCompressedStreamAsNecessary(
                new BufferedOutputStream(new FileOutputStream(tempFile)),
                compressEnabled,
                this.verbose
            );
            outputWriter = new OutputStreamWriter(os);
            if (verbose) {
                VansLogger.logger.debug(String.format("Collecting content into %s before uploading.", tempFile));
            }

        } catch (Exception ex) {
            if (verbose) {
                VansLogger.logger.error("Cannot start publish batch", ex);
            }
            throw new RuntimeException(String.format("Cannot start publishing: %s", ex.getMessage()), ex);
        }
    }

    @Override
    public void publish(PublishContext context, int sequence, Event event) {
        try {
            outputWriter.write(event.getMessage());
        } catch (Exception ex) {
            if (verbose) {
                VansLogger.logger.error("Cannot publish batch", ex);
            }
            throw new RuntimeException(String.format("Cannot collect event %s: %s", event, ex.getMessage()), ex);
        }
    }

    @Override
    public void end(PublishContext context) {
        try {
            if (null != outputWriter) {
                outputWriter.close();
                outputWriter = null;
            }
            publishFile(tempFile, context);
        } catch (Exception ex) {
            if (verbose) {
                VansLogger.logger.error("Cannot end publish batch", ex);
            }
            throw new RuntimeException(String.format("Cannot end publishing: %s", ex.getMessage()), ex);
        } finally {
            if (null != tempFile) {
                try {
                    tempFile.delete();
                    tempFile = null;
                } catch (Exception ex) {
                }
            }
        }
    }

    protected abstract void publishFile(File file, PublishContext context) throws Exception;

    static OutputStream createCompressedStreamAsNecessary(
            OutputStream outputStream, boolean compressEnabled, boolean verbose) throws IOException {
        Objects.requireNonNull(outputStream);
        if (compressEnabled) {
            if (verbose) {
                VansLogger.logger.debug("Content will be compressed.");
            }
            return new GZIPOutputStream(outputStream);
        } else {
            return outputStream;
        }
    }
}
