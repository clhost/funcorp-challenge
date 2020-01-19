package com.clhost.memes.app.tree;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@FeignClient(value = "${service.tree.name:vp-tree}", configuration = TreeClientConfiguration.class)
public interface TreeClient {
    @RequestMapping(value = "/tree/putAsync", method = RequestMethod.POST, produces = MediaType.APPLICATION_JSON_VALUE)
    void putAsync(@RequestBody MetaMeme metaMeme, @RequestHeader("X-CID") String cid);
}
