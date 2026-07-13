/**
 * Labelled wrapper for *custom* form content.
 * Use this when you need a label/hint over something other than a
 * single plain input — e.g. a TextArea, a select, a row of inputs.
 *
 * For a labelled single <input>, use FormField instead.
 */
const Field = ({ label, required, hint, error, children, className }) => (
  <div className={'w-full' + (className ? ' ' + className : '')}>
    {label && (
      <label className="label">
        {label}
        {required && <span className="ml-0.5 text-red-500">*</span>}
      </label>
    )}
    {children}
    {error ? (
      <p className="error-text">{error}</p>
    ) : hint ? (
      <p className="helper-text">{hint}</p>
    ) : null}
  </div>
);

export default Field;