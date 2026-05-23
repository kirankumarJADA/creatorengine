import { cn } from '../../utils/helpers.js';

const Skeleton = ({ className, ...rest }) => (
  <div
    className={cn('skeleton h-4 w-full', className)}
    aria-hidden="true"
    {...rest}
  />
);

export const SkeletonText = ({ lines = 3, className }) => (
  <div className={cn('space-y-2', className)}>
    {Array.from({ length: lines }).map((_, i) => (
      <Skeleton
        key={i}
        className={i === lines - 1 ? 'h-3.5 w-2/3' : 'h-3.5 w-full'}
      />
    ))}
  </div>
);

export const SkeletonCard = ({ className }) => (
  <div className={cn('card p-5 sm:p-6', className)}>
    <div className="flex items-center gap-3">
      <Skeleton className="h-10 w-10 rounded-xl" />
      <div className="flex-1 space-y-2">
        <Skeleton className="h-3.5 w-1/3" />
        <Skeleton className="h-3 w-1/2" />
      </div>
    </div>
    <Skeleton className="mt-5 h-8 w-24" />
  </div>
);

export default Skeleton;
