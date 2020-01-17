package com.clhost.memes.app.api;

import com.clhost.memes.app.api.model.ApiFault;
import com.clhost.memes.app.api.model.ContentItem;
import com.clhost.memes.app.api.model.PreviewItem;
import com.clhost.memes.app.dao.MemesDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
public class FeedController implements FeedApi {
    private static final Logger LOGGER = LogManager.getLogger(FeedController.class);
    private static final String DEFAULT_LANG = "de";

    private final MemesDao memesDao;

    @Value("${service.feed.page-item-count}")
    private int pageItemCount;

    @Autowired
    public FeedController(MemesDao memesDao) {
        this.memesDao = memesDao;
    }

    @Override
    public List<PreviewItem> feed(Long page, String lang) throws ApiFault {
        try {
            return memesDao.memesPage(pageItemCount, offset(page), lang(lang));
        } catch (Exception e) {
            throw new ApiFault(e.getMessage());
        }
    }

    @Override
    public ContentItem contentItem(String contentId) throws ApiFault {
        try {
            return memesDao.contentItemInfo(contentId);
        } catch (Exception e) {
            throw new ApiFault(e.getMessage());
        }
    }

    private String lang(String lang) {
        return lang == null || lang.trim().equals("") ? DEFAULT_LANG : lang;
    }

    private long offset(Long page) {
        return page == null || page <= 0 ? 0 : (page - 1) * pageItemCount;
    }
}
