import { Component } from 'react';
import { AlertTriangle, RotateCw } from 'lucide-react';

/**
 * Catches render errors in the page tree and shows a friendly fallback
 * instead of a blank screen. Wrapped around the routed <Outlet/> with a
 * key that changes on navigation, so moving to another page auto-recovers.
 */
class ErrorBoundary extends Component {
  constructor(props) {
    super(props);
    this.state = { hasError: false };
  }

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    console.error('Page render error:', error, info);
  }

  handleReload = () => {
    this.setState({ hasError: false });
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return (
        <div className="flex min-h-[60vh] flex-col items-center justify-center px-6 text-center">
          <span className="mb-4 grid h-14 w-14 place-items-center rounded-2xl bg-amber-100 text-amber-700 dark:bg-amber-500/10 dark:text-amber-400">
            <AlertTriangle size={26} />
          </span>
          <h2 className="text-lg font-semibold text-ink-900 dark:text-ink-100">
            Something went wrong on this page
          </h2>
          <p className="mt-1 max-w-sm text-sm text-ink-500 dark:text-ink-400">
            This is just a display hiccup — your automations are still running.
            Reload to get back to it.
          </p>
          <button
            type="button"
            onClick={this.handleReload}
            className="mt-5 inline-flex items-center gap-2 rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-brand-700"
          >
            <RotateCw size={16} />
            Reload page
          </button>
        </div>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;