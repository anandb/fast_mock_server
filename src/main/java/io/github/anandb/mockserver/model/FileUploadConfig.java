package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * Configuration for saving uploaded multipart files to disk.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileUploadConfig {
    /**
     * Directory path to save uploaded files (supports ${pathVariables.xxx} expansion).
     */
    private String saveTo;
    /**
     * Optional filter — only save files from these form field names. If empty, all file parts are saved.
     */
    private List<String> fileFields;

    /**
     * Directory path to save uploaded files (supports ${pathVariables.xxx} expansion).
     */
    public String getSaveTo() {
        return this.saveTo;
    }

    /**
     * Optional filter — only save files from these form field names. If empty, all file parts are saved.
     */
    public List<String> getFileFields() {
        return this.fileFields;
    }

    /**
     * Directory path to save uploaded files (supports ${pathVariables.xxx} expansion).
     */
    public void setSaveTo(final String saveTo) {
        this.saveTo = saveTo;
    }

    /**
     * Optional filter — only save files from these form field names. If empty, all file parts are saved.
     */
    public void setFileFields(final List<String> fileFields) {
        this.fileFields = fileFields;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof FileUploadConfig)) return false;
        final FileUploadConfig other = (FileUploadConfig) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$saveTo = this.getSaveTo();
        final Object other$saveTo = other.getSaveTo();
        if (this$saveTo == null ? other$saveTo != null : !this$saveTo.equals(other$saveTo)) return false;
        final Object this$fileFields = this.getFileFields();
        final Object other$fileFields = other.getFileFields();
        if (this$fileFields == null ? other$fileFields != null : !this$fileFields.equals(other$fileFields)) return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof FileUploadConfig;
    }

    @Override
    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $saveTo = this.getSaveTo();
        result = result * PRIME + ($saveTo == null ? 43 : $saveTo.hashCode());
        final Object $fileFields = this.getFileFields();
        result = result * PRIME + ($fileFields == null ? 43 : $fileFields.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "FileUploadConfig(saveTo=" + this.getSaveTo() + ", fileFields=" + this.getFileFields() + ")";
    }

    public FileUploadConfig() {
    }

    /**
     * Creates a new {@code FileUploadConfig} instance.
     *
     * @param saveTo Directory path to save uploaded files (supports ${pathVariables.xxx} expansion).
     * @param fileFields Optional filter — only save files from these form field names. If empty, all file parts are saved.
     */
    public FileUploadConfig(final String saveTo, final List<String> fileFields) {
        this.saveTo = saveTo;
        this.fileFields = fileFields;
    }
}
