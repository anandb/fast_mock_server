package io.github.anandb.mockserver.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Configuration for saving uploaded multipart files to disk.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FileUploadConfig {

    /** Directory path to save uploaded files (supports ${pathVariables.xxx} expansion). */
    private String saveTo;

    /** Optional filter — only save files from these form field names. If empty, all file parts are saved. */
    private List<String> fileFields;
}
