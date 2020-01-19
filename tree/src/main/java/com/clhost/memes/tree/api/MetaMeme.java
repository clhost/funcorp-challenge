package com.clhost.memes.tree.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MetaMeme {
    @JsonProperty("text")
    private String text;

    @NotNull
    @JsonProperty("lang")
    private String lang;

    @NotNull
    @JsonProperty("source")
    private String source;

    @NotNull
    @JsonProperty("urls")
    private List<String> urls;
}
