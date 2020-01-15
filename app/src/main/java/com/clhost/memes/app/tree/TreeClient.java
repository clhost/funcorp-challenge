package com.clhost.memes.app.tree;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.validation.Valid;

@FeignClient("${service.tree.name:vp-tree}")
public interface TreeClient {
    @RequestMapping(value = "/tree/putAsync", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    void putAsync(@RequestBody @Valid MetaMeme metaMeme);
}
