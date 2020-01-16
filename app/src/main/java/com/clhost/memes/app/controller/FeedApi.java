package com.clhost.memes.app.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface FeedApi {
    @RequestMapping(value = "/feed", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    List<PreviewItem> feed(@RequestParam(value = "page", required = false) Long page,
                           @RequestParam(value = "lang", required = false) String lang) throws ApiFault;

    @RequestMapping(value = "/feed/{content_id}", method = RequestMethod.GET, produces = MediaType.APPLICATION_JSON_VALUE)
    ContentItem contentItem(@PathVariable("content_id") String contentId) throws ApiFault;
}
