package io.github.xwz.sbs.api;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.github.xwz.base.ImmutableMap;
import io.github.xwz.base.content.IContentManager;
import io.github.xwz.base.models.IEpisodeModel;
import io.github.xwz.sbs.content.ContentManager;

public class SBSRelatedApi extends SBSApiBase {
    private static final String TAG = "SBSRelatedApi";
    private static final int CACHE_EXPIRY = 3600; // 1h

    private final String id;
    private boolean success = false;

    public SBSRelatedApi(Context context, String id) {
        super(context);
        this.id = id;
    }

    @Override
    protected Void doInBackground(String... urls) {
        if (urls.length > 0) {
            updateEpisode(urls[0]);
        }
        return null;
    }

    private boolean updateEpisode(String url) {
        EpisodeModel current = (EpisodeModel) ContentManager.getInstance().getEpisode(url);
        if (current != null) {
            Log.d(TAG, "Fetched related info for: " + current);
            String series = current.getSeriesTitle();
            List<IEpisodeModel> related = fetchRelated(url);
            if (related != null) {
                List<IEpisodeModel> more = new ArrayList<>();
                for (IEpisodeModel ep : related) {
                    if (series != null && !series.equals(ep.getSeriesTitle())) {
                        more.add(ep);
                    }
                }
                if (more.size() > 0) {
                    current.setOtherEpisodes(IContentManager.MORE_LIKE_THIS, more);
                }
                current.setHasExtra(true);
                current.setHasFetchedRelated(true);
                success = true;
            }
        } else {
            Log.d(TAG, "Unable to find current episode");
        }
        return false;
    }

    private List<IEpisodeModel> fetchRelated(String url) {
        String id = getIdFromUrl(url);
        if (id != null) {
            List<IEpisodeModel> all = new ArrayList<>();
            List<IEpisodeModel> related = fetchContent(getRelatedUrl(id), CACHE_EXPIRY);
            for (IEpisodeModel ep : related) {

                // load from existing cache if possible, has better data.
                IEpisodeModel info = ContentManager.getInstance().getEpisode(ep.getHref());
                if (info != null) {
                    all.add(info);
                } else {
                    all.add(ep);
                }
            }
            return all;
        }
        return null;
    }

    private Uri getRelatedUrl(String id) {
        Map<String, String> params = ImmutableMap.of("context", "android", "form", "json", "id", id);
        return buildRelatedUrl(params);
    }

    protected void onPreExecute() {
        ContentManager.getInstance().broadcastChange(ContentManager.CONTENT_EPISODE_START, id);
    }

    protected void onPostExecute(Void v) {
        if (success) {
            ContentManager.getInstance().broadcastChange(ContentManager.CONTENT_EPISODE_DONE, id);
        } else {
            ContentManager.getInstance().broadcastChange(ContentManager.CONTENT_EPISODE_ERROR, id);
        }
    }
}