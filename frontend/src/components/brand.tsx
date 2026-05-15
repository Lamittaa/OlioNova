import { useState } from "react";
import { useI18n } from "../app/i18n-context";
import { publicAsset } from "../lib/assets";
import { cx } from "../lib/utils";

type BrandSize = "sm" | "md" | "hero";
type BrandLayout = "inline" | "stacked";
type BrandVariant = "logo" | "emblem";

export function BrandMark(props: {
  className?: string;
  inverse?: boolean;
  layout?: BrandLayout;
  showWordmark?: boolean;
  size?: BrandSize;
  variant?: BrandVariant;
}): JSX.Element {
  const { t } = useI18n();
  const variant = props.variant ?? "logo";
  const [assetMode, setAssetMode] = useState<"press" | "brand" | "fallback">("press");

  const pressSrc = variant === "emblem" ? publicAsset("press-emblem.png") : publicAsset("press-logo.png");
  const logoSrc = assetMode === "press" ? pressSrc : publicAsset("brand-logo.png");

  return (
    <div
      className={cx(
        "brand-mark",
        `brand-mark-${props.layout ?? "inline"}`,
        `brand-mark-${props.size ?? "md"}`,
        `brand-mark-${variant}`,
        props.inverse && "brand-mark-inverse",
        props.className
      )}
    >
      <div className="brand-visual">
        {assetMode !== "fallback" ? (
          <img
            alt={t("Olive Press logo")}
            className="brand-logo"
            onError={() => setAssetMode((current) => (current === "press" ? "brand" : "fallback"))}
            src={logoSrc}
          />
        ) : null}
        {assetMode === "fallback" ? (
          <div className="brand-fallback">
            <span>OPS</span>
          </div>
        ) : null}
      </div>
      {props.showWordmark ? (
        <div className="brand-wordmark">
          <strong>{t("Olive Press Management")}</strong>
          <span>{t("Operations Platform")}</span>
        </div>
      ) : null}
    </div>
  );
}
