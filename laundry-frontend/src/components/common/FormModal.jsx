import { useState, useEffect } from "react";
import { X, Eye, EyeOff, UserCircle2 } from "lucide-react";
import Button from "./Button";

const FormModal = ({
  isOpen,
  title,
  subtitle,
  fields = [],
  initialData = {},
  mode = "create",
  onSubmit,
  onClose,
  validate,
  renderHeader,
}) => {
  const [formData, setFormData] = useState({});
  const [errors, setErrors] = useState({});
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState({});

  useEffect(() => {
    if (!isOpen) return;
    const defaults = {};
    fields.forEach((f) => {
      defaults[f.key] =
        initialData[f.key] ??
        f.defaultValue ??
        (f.type === "toggle" ? true : f.type === "checkbox-group" ? [] : "");
    });
    setFormData(defaults);
    setErrors({});
    setShowPassword({});
    setLoading(false);
  }, [isOpen]);

  if (!isOpen) return null;

  const setValue = (key, value) => {
    setFormData((prev) => ({ ...prev, [key]: value }));
    if (errors[key]) setErrors((prev) => ({ ...prev, [key]: null }));
  };

  const validateField = (key, value) => {
    if (!validate) return;
    const all = validate({ ...formData, [key]: value }, mode);
    setErrors((prev) => ({ ...prev, [key]: all[key] ?? null }));
  };

  const handleSubmit = async (e) => {
    e?.preventDefault();
    const allErrors = validate ? validate(formData, mode) : {};
    if (Object.keys(allErrors).length) {
      setErrors(allErrors);
      return;
    }

    setLoading(true);
    try {
      await onSubmit(formData, mode);
    } catch {
      // caller shows toast on error
    } finally {
      setLoading(false);
    }
  };

  const baseInput =
    "w-full py-2.5 text-sm border rounded-xl focus:outline-none focus:ring-2 transition-colors disabled:opacity-50";
  const errorCls = (key) =>
    errors[key]
      ? "border-red-300 bg-red-50/50 focus:border-red-400 focus:ring-red-100"
      : "border-slate-200 bg-white focus:border-blue-400 focus:ring-blue-100";

  const renderField = (field) => {
    const {
      key,
      type,
      placeholder,
      options,
      hint,
      prefixIcon: PrefixIcon,
    } = field;
    const value = formData[key] ?? "";

    if (type === "toggle") {
      return (
        <div className="flex items-center justify-between p-3.5 bg-slate-50 rounded-xl border border-slate-100">
          <div>
            <p className="text-sm font-semibold text-slate-700">
              {field.label}
            </p>
            {hint && <p className="text-[11px] text-slate-400">{hint}</p>}
          </div>
          <label className="relative inline-flex items-center cursor-pointer flex-shrink-0">
            <input
              type="checkbox"
              checked={!!value}
              onChange={(e) => setValue(key, e.target.checked)}
              className="sr-only peer"
            />
            <div
              className="w-10 h-5 bg-slate-300 rounded-full peer peer-checked:bg-blue-600 transition-colors
              after:content-[''] after:absolute after:top-0.5 after:left-0.5
              after:bg-white after:rounded-full after:w-4 after:h-4
              after:transition-all peer-checked:after:translate-x-5
              peer-focus:ring-2 peer-focus:ring-blue-100"
            />
          </label>
        </div>
      );
    }

    if (type === "checkbox-group") {
      const selected = Array.isArray(value) ? value : [];
      const toggleOption = (optValue) => {
        const next = selected.includes(optValue)
          ? selected.filter((v) => v !== optValue)
          : [...selected, optValue];
        setValue(key, next);
        if (validate) {
          const all = validate({ ...formData, [key]: next }, mode);
          setErrors((prev) => ({ ...prev, [key]: all[key] ?? null }));
        }
      };
      return (
        <>
          <div className="flex flex-wrap gap-2">
            {options?.map((opt) => {
              const active = selected.includes(opt.value);
              return (
                <button
                  key={opt.value}
                  type="button"
                  disabled={loading}
                  onClick={() => toggleOption(opt.value)}
                  className={[
                    "px-3 py-1.5 rounded-lg text-xs font-semibold border transition-colors disabled:opacity-50",
                    active
                      ? "bg-blue-600 border-blue-600 text-white"
                      : "bg-white border-slate-200 text-slate-600 hover:border-blue-300 hover:text-blue-600",
                  ].join(" ")}
                >
                  {opt.label}
                </button>
              );
            })}
          </div>
          {errors[key] && (
            <p className="mt-1 text-[11px] text-red-500 font-medium">
              {errors[key]}
            </p>
          )}
        </>
      );
    }

    if (type === "select") {
      return (
        <>
          <div className="relative">
            {PrefixIcon && (
              <PrefixIcon
                size={14}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none"
              />
            )}
            <select
              value={value}
              onChange={(e) => setValue(key, e.target.value)}
              onBlur={() => validateField(key, value)}
              disabled={loading}
              className={[
                baseInput,
                PrefixIcon ? "pl-9 pr-3" : "px-3",
                "appearance-none cursor-pointer",
                errorCls(key),
              ].join(" ")}
            >
              <option value="">Sélectionner…</option>
              {options?.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>
          {errors[key] && (
            <p className="mt-1 text-[11px] text-red-500 font-medium">
              {errors[key]}
            </p>
          )}
        </>
      );
    }

    if (type === "password") {
      const visible = showPassword[key];
      return (
        <>
          <div className="relative">
            {PrefixIcon && (
              <PrefixIcon
                size={14}
                className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none"
              />
            )}
            <input
              type={visible ? "text" : "password"}
              value={value}
              onChange={(e) => setValue(key, e.target.value)}
              onBlur={() => validateField(key, value)}
              placeholder={placeholder}
              disabled={loading}
              className={[
                baseInput,
                PrefixIcon ? "pl-9" : "px-3",
                "pr-10",
                errorCls(key),
              ].join(" ")}
            />
            <button
              type="button"
              tabIndex={-1}
              onClick={() => setShowPassword((p) => ({ ...p, [key]: !p[key] }))}
              className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 hover:text-slate-600 transition-colors"
            >
              {visible ? <EyeOff size={14} /> : <Eye size={14} />}
            </button>
          </div>
          {errors[key] && (
            <p className="mt-1 text-[11px] text-red-500 font-medium">
              {errors[key]}
            </p>
          )}
        </>
      );
    }

    // text / email / number
    return (
      <>
        <div className="relative">
          {PrefixIcon && (
            <PrefixIcon
              size={14}
              className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400 pointer-events-none"
            />
          )}
          <input
            type={type}
            value={value}
            onChange={(e) => setValue(key, e.target.value)}
            onBlur={() => validateField(key, value)}
            placeholder={placeholder}
            disabled={loading}
            className={[
              baseInput,
              PrefixIcon ? "pl-9 pr-3" : "px-3",
              errorCls(key),
            ].join(" ")}
          />
        </div>
        {errors[key] && (
          <p className="mt-1 text-[11px] text-red-500 font-medium">
            {errors[key]}
          </p>
        )}
      </>
    );
  };

  // Regroupe les champs ayant la même propriété `group` côte à côte
  const renderFields = () => {
    const result = [];
    let i = 0;
    while (i < fields.length) {
      const field = fields[i];
      const nextField = fields[i + 1];

      if (field.group && nextField?.group === field.group) {
        result.push(
          <div
            key={`${field.key}-${nextField.key}`}
            className="grid grid-cols-2 gap-3"
          >
            <FieldWrapper field={field} hint={undefined}>
              {renderField(field)}
            </FieldWrapper>
            <FieldWrapper field={nextField} hint={undefined}>
              {renderField(nextField)}
            </FieldWrapper>
          </div>,
        );
        i += 2;
      } else {
        result.push(
          <FieldWrapper key={field.key} field={field} hint={field.hint}>
            {renderField(field)}
          </FieldWrapper>,
        );
        i += 1;
      }
    }
    return result;
  };

  return (
    <div
      className="fixed inset-0 bg-black/40 backdrop-blur-sm z-50 flex items-center justify-center p-4"
      onClick={(e) => !loading && e.target === e.currentTarget && onClose()}
    >
      <div className="bg-white rounded-2xl shadow-2xl shadow-black/10 w-full max-w-lg max-h-[92vh] overflow-y-auto animate-fade-in-up">
        {/* ── En-tête (custom ou défaut) ─────────────────────────── */}
        {renderHeader ? (
          renderHeader(formData, onClose, loading)
        ) : (
          <div className="relative overflow-hidden rounded-t-2xl bg-gradient-to-br from-slate-800 to-slate-900 px-6 pt-6 pb-5">
            <div className="absolute -right-4 -top-4 w-24 h-24 rounded-full bg-white/5" />
            <div className="absolute right-8 bottom-0 w-12 h-12 rounded-full bg-white/5" />
            <div className="relative flex items-start justify-between gap-3">
              <div className="flex items-center gap-3.5">
                <div className="w-11 h-11 rounded-xl bg-white/10 flex items-center justify-center flex-shrink-0 ring-1 ring-white/10">
                  <UserCircle2 size={22} className="text-white/70" />
                </div>
                <div>
                  <h2 className="font-bold text-white text-[15px] leading-tight">
                    {title}
                  </h2>
                  {subtitle && (
                    <p className="text-[11px] text-slate-400 mt-0.5">
                      {subtitle}
                    </p>
                  )}
                </div>
              </div>
              <button
                onClick={onClose}
                disabled={loading}
                className="p-1.5 text-slate-500 hover:text-white hover:bg-white/10 rounded-lg transition-colors mt-0.5 flex-shrink-0"
              >
                <X size={16} />
              </button>
            </div>
          </div>
        )}

        {/* ── Champs ─────────────────────────────────────────────── */}
        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {renderFields()}
        </form>

        {/* ── Boutons ────────────────────────────────────────────── */}
        <div className="flex gap-2.5 px-6 pb-6">
          <Button
            variant="secondary"
            onClick={onClose}
            disabled={loading}
            className="flex-1"
          >
            Annuler
          </Button>
          <Button
            type="button"
            variant="primary"
            loading={loading}
            onClick={handleSubmit}
            className="flex-1"
          >
            {mode === "create" ? "Créer" : "Sauvegarder"}
          </Button>
        </div>
      </div>
    </div>
  );
};

const FieldWrapper = ({ field, hint, children }) => {
  if (field.type === "toggle") return <>{children}</>;
  return (
    <div>
      <div className="flex items-center justify-between mb-1.5">
        <label className="text-xs font-semibold text-slate-600">
          {field.label}
          {field.required && <span className="text-red-400 ml-0.5">*</span>}
        </label>
        {hint && (
          <span className="text-[10px] text-slate-400 italic">{hint}</span>
        )}
      </div>
      {children}
    </div>
  );
};

export default FormModal;
