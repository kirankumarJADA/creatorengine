import { useEffect, useState } from 'react';
import { Check, Image as ImageIcon, Images, RefreshCw } from 'lucide-react';

import { useBuilderStore } from '../../store/builderStore.js';
import instagramService from '../../services/instagramService.js';

/**
 * Post-picker — shown inside the Trigger step when the automation
 * triggers on comments. Scopes the automation to one specific post,
 * or leaves it on "All posts" (targetPostId = null).
 *
 * Renders nothing for non-comment triggers (DMs have no post context).
 */

const isCommentTrigger = (t) =>
  String(t ?? '').toUpperCase().includes('COMMENT');

const thumbOf = (post) =>
  post.thumbnailUrl || post.thumbnail_url || post.mediaUrl || post.media_url || null;

const SelectedBadge = () => (
  <span className="absolute right-1.5 top-1.5 flex h-5 w-5 items-center justify-center rounded-full bg-brand-500 text-white shadow">
    <Check className="h-3 w-3" strokeWidth={3} />
  </span>
);

const PostPicker = () => {
  const trigger         = useBuilderStore((s) => s.draft.trigger);
  const targetPostId    = useBuilderStore((s) => s.draft.targetPostId);
  const setTargetPostId = useBuilderStore((s) => s.setTargetPostId);

  const [posts, setPosts]     = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState(null);
  const [reloadKey, setReloadKey] = useState(0);

  const show = isCommentTrigger(trigger);

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

  if (!show) return null;

  return (
    <div className="mt-8 border-t border-ink-100 pt-6 dark:border-ink-800">
      <div className="mb-3 flex items-start justify-between gap-3">
        <div>
          <h3 className="text-sm font-semibold text-ink-900 dark:text-ink-100">
            Which post?
          </h3>
          <p className="mt-0.5 text-xs text-ink-500 dark:text-ink-400">
            Run this only on one post's comments, or keep{' '}
            <span className="font-medium">All posts</span> to run on every post.
          </p>
        </div>
        {!loading && (
          <button
            type="button"
            onClick={() => setReloadKey((k) => k + 1)}
            className="flex shrink-0 items-center gap-1 rounded-lg px-2 py-1 text-xs font-medium text-ink-500 hover:bg-ink-100 hover:text-ink-700 dark:text-ink-400 dark:hover:bg-ink-800 dark:hover:text-ink-200"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Refresh
          </button>
        )}
      </div>

      {loading ? (
        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:grid-cols-4">
          {Array.from({ length: 8 }).map((_, i) => (
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
            {/* All posts */}
            <button
              type="button"
              onClick={() => setTargetPostId(null)}
              className={`relative flex aspect-square flex-col items-center justify-center gap-2 rounded-xl border-2 p-3 text-center transition ${
                !targetPostId
                  ? 'border-brand-500 bg-brand-50 dark:border-brand-500 dark:bg-brand-500/10'
                  : 'border-ink-200 hover:border-ink-300 dark:border-ink-700 dark:hover:border-ink-600'
              }`}
            >
              <Images className="h-6 w-6 text-brand-600 dark:text-brand-400" />
              <span className="text-xs font-medium text-ink-700 dark:text-ink-200">
                All posts
              </span>
              {!targetPostId && <SelectedBadge />}
            </button>

            {/* Each post */}
            {posts.map((post) => {
              const selected = targetPostId === post.id;
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