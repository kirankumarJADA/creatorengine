import { useEffect, useState } from 'react';
import { Check, ChevronDown, Image as ImageIcon, Images, RefreshCw } from 'lucide-react';

import { useBuilderStore } from '../../store/builderStore.js';
import instagramService from '../../services/instagramService.js';
import { POST_TARGET_MODE, TRIGGER_TYPE } from '../../utils/constants.js';

const INITIAL_POSTS_SHOWN = 2;

const thumbOf = (post) =>
  post.thumbnailUrl || post.thumbnail_url || post.mediaUrl || post.media_url || null;

const SelectedBadge = () => (
  <span className="absolute right-1.5 top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-brand-500 text-white shadow">
    <Check className="h-3 w-3" strokeWidth={3} />
  </span>
);

const PostPicker = () => {
  const trigger            = useBuilderStore((s) => s.draft.trigger);
  const targetPostMode     = useBuilderStore((s) => s.draft.targetPostMode);
  const targetPostId       = useBuilderStore((s) => s.draft.targetPostId);
  const setTargetPostMode  = useBuilderStore((s) => s.setTargetPostMode);
  const setTargetPostId    = useBuilderStore((s) => s.setTargetPostId);

  const [posts, setPosts]     = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState(null);
  const [reloadKey, setReloadKey] = useState(0);
  const [showAll, setShowAll] = useState(false);

  // Only show the post picker for the regular COMMENT trigger.
  const show = trigger === TRIGGER_TYPE.COMMENT;

  useEffect(() => {
    if (!show) return undefined;
    let cancelled = false;

    (async () => {
      setLoading(true);
      setError(null);
      try {
        const items = await instagramService.getMedia();
        if (!cancelled) setPosts(Array.isArray(items) ? items : []);
      } catch (e) {
        if (!cancelled) {
          setError('Could not load your posts. Make sure your Instagram account is connected.');
        }
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();

    return () => { cancelled = true; };
  }, [show, reloadKey]);

  // If a user is editing and their selected post is beyond the first 2,
  // auto-expand so the highlight is visible.
  useEffect(() => {
    if (!targetPostId || posts.length === 0) return;
    const idx = posts.findIndex((p) => p.id === targetPostId);
    if (idx >= INITIAL_POSTS_SHOWN) setShowAll(true);
  }, [targetPostId, posts]);

  if (!show) return null;

  const isAll      = targetPostMode === POST_TARGET_MODE.ALL || (!targetPostMode && !targetPostId);
  const isSpecific = targetPostMode === POST_TARGET_MODE.SPECIFIC || (!targetPostMode && !!targetPostId);

  const visiblePosts = showAll ? posts : posts.slice(0, INITIAL_POSTS_SHOWN);
  const hiddenCount  = posts.length - INITIAL_POSTS_SHOWN;
  const hasMore      = hiddenCount > 0 && !showAll;

  return (
    <div className="mt-8 border-t border-ink-100 pt-6 dark:border-ink-800">
      <div className="mb-3 flex items-start justify-between gap-3">
        <div>
          <h3 className="text-sm font-semibold text-ink-900 dark:text-ink-100">
            Which post?
          </h3>
          <p className="mt-0.5 text-xs text-ink-500 dark:text-ink-400">
            Run this on every post, or pick one specific post.
          </p>
        </div>
        {!loading && (
          <button
            type="button"
            onClick={() => { setReloadKey((k) => k + 1); setShowAll(false); }}
            className="flex shrink-0 items-center gap-1 rounded-lg px-2 py-1 text-xs font-medium text-ink-500 hover:bg-ink-100 hover:text-ink-700 dark:text-ink-400 dark:hover:bg-ink-800 dark:hover:text-ink-200"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Refresh
          </button>
        )}
      </div>

      {loading ? (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
          {Array.from({ length: 3 }).map((_, i) => (
            <div
              key={i}
              className="aspect-square animate-pulse rounded-xl bg-ink-100 dark:bg-ink-800"
            />
          ))}
        </div>
      ) : error ? (
        <div className="rounded-xl border border-ink-200 p-4 text-sm text-ink-600 dark:border-ink-700 dark:text-ink-300">
          {error}{' '}
          <button
            type="button"
            onClick={() => setReloadKey((k) => k + 1)}
            className="font-medium text-brand-600 hover:underline dark:text-brand-400"
          >
            Retry
          </button>
        </div>
      ) : (
        <>
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
            <button
              type="button"
              onClick={() => setTargetPostMode(POST_TARGET_MODE.ALL)}
              className={`relative flex aspect-square flex-col items-center justify-center gap-2 rounded-xl border-2 p-3 text-center transition ${
                isAll
                  ? 'border-brand-500 bg-brand-50 dark:border-brand-500 dark:bg-brand-500/10'
                  : 'border-ink-200 hover:border-ink-300 dark:border-ink-700 dark:hover:border-ink-600'
              }`}
            >
              <Images className="h-6 w-6 text-brand-600 dark:text-brand-400" />
              <span className="text-xs font-medium text-ink-700 dark:text-ink-200">
                All posts
              </span>
              {isAll && <SelectedBadge />}
            </button>

            {visiblePosts.map((post) => {
              const selected = isSpecific && targetPostId === post.id;
              const thumb = thumbOf(post);
              return (
                <button
                  key={post.id}
                  type="button"
                  onClick={() => setTargetPostId(post.id)}
                  title={post.caption || ''}
                  className={`relative aspect-square overflow-hidden rounded-xl border-2 transition ${
                    selected
                      ? 'border-brand-500'
                      : 'border-transparent hover:border-ink-300 dark:hover:border-ink-600'
                  }`}
                >
                  {thumb ? (
                    <img
                      src={thumb}
                      alt={post.caption ? post.caption.slice(0, 40) : 'Instagram post'}
                      className="h-full w-full object-cover"
                      loading="lazy"
                    />
                  ) : (
                    <div className="flex h-full w-full items-center justify-center bg-ink-100 dark:bg-ink-800">
                      <ImageIcon className="h-6 w-6 text-ink-400" />
                    </div>
                  )}
                  {selected && <SelectedBadge />}
                </button>
              );
            })}
          </div>

          {hasMore && (
            <button
              type="button"
              onClick={() => setShowAll(true)}
              className="mt-3 flex w-full items-center justify-center gap-1.5 rounded-xl border border-dashed border-ink-200 py-2.5 text-sm font-medium text-ink-600 transition-colors hover:border-brand-400 hover:text-brand-700 dark:border-ink-700 dark:text-ink-300 dark:hover:border-brand-500 dark:hover:text-brand-400"
            >
              <ChevronDown className="h-4 w-4" />
              Show {hiddenCount} more post{hiddenCount === 1 ? '' : 's'}
            </button>
          )}

          {showAll && posts.length > INITIAL_POSTS_SHOWN && (
            <button
              type="button"
              onClick={() => setShowAll(false)}
              className="mt-3 flex w-full items-center justify-center gap-1.5 rounded-xl border border-dashed border-ink-200 py-2.5 text-sm font-medium text-ink-600 transition-colors hover:border-brand-400 hover:text-brand-700 dark:border-ink-700 dark:text-ink-300 dark:hover:border-brand-500 dark:hover:text-brand-400"
            >
              <ChevronDown className="h-4 w-4 rotate-180" />
              Show less
            </button>
          )}

          {posts.length === 0 && (
            <p className="mt-3 text-xs text-ink-500 dark:text-ink-400">
              No posts found on your connected account yet — "All posts" will still work.
            </p>
          )}
        </>
      )}
    </div>
  );
};

export default PostPicker;