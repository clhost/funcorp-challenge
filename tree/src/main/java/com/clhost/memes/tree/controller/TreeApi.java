package com.clhost.memes.tree.controller;

import com.clhost.memes.tree.data.MetaMeme;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;

public interface TreeApi {
    @RequestMapping(value = "/tree/putAsync", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    void putAsync(@RequestBody @Valid MetaMeme metaMeme);
}
