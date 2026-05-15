import {
  useEffect,
  useMemo,
  useRef,
  useState,
  type ChangeEvent,
  type DragEvent
} from "react";

type CameraCaptureSource = "camera" | "upload";

type CameraCaptureInputProps = Readonly<{
  id: string;
  language: "ar" | "en";
  file: File | null;
  onFileChange: (file: File | null, source: CameraCaptureSource) => void;
  disabled?: boolean;
}>;

function isLiveCameraSupported(): boolean {
  return typeof navigator !== "undefined" && Boolean(navigator.mediaDevices?.getUserMedia);
}

function stopMediaStream(stream: MediaStream | null): void {
  stream?.getTracks().forEach((track) => track.stop());
}

function getMessage(language: "ar" | "en", arabic: string, english: string): string {
  return language === "ar" ? arabic : english;
}

export function CameraCaptureInput({
  id,
  language,
  file,
  onFileChange,
  disabled = false
}: CameraCaptureInputProps): JSX.Element {
  const videoRef = useRef<HTMLVideoElement | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  const cameraFallbackInputRef = useRef<HTMLInputElement | null>(null);
  const streamRef = useRef<MediaStream | null>(null);

  const [cameraOpen, setCameraOpen] = useState(false);
  const [cameraError, setCameraError] = useState("");
  const [previewUrl, setPreviewUrl] = useState("");
  const [sourceLabel, setSourceLabel] = useState<CameraCaptureSource | null>(null);

  const liveCameraSupported = useMemo(() => isLiveCameraSupported(), []);

  useEffect(() => {
    if (!file) {
      setPreviewUrl("");
      setSourceLabel(null);
      return;
    }

    const objectUrl = URL.createObjectURL(file);
    setPreviewUrl(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [file]);

  useEffect(() => {
    if (!cameraOpen || !videoRef.current || !streamRef.current) {
      return;
    }

    videoRef.current.srcObject = streamRef.current;
    void videoRef.current.play().catch(() => {
      setCameraError(
        getMessage(
          language,
          "تعذر تشغيل معاينة الكاميرا. استخدم الرفع اليدوي كبديل.",
          "Could not start the camera preview. Use manual upload instead."
        )
      );
    });
  }, [cameraOpen, language]);

  useEffect(() => {
    return () => {
      stopMediaStream(streamRef.current);
    };
  }, []);

  const closeCamera = (): void => {
    stopMediaStream(streamRef.current);
    streamRef.current = null;
    setCameraOpen(false);
  };

  const openCamera = async (): Promise<void> => {
    if (disabled) {
      return;
    }

    setCameraError("");

    if (!liveCameraSupported) {
      cameraFallbackInputRef.current?.click();
      setCameraError(
        getMessage(
          language,
          "هذا المتصفح لا يدعم فتح الكاميرا مباشرة. يمكنك اختيار صورة أو استخدام كاميرا الجهاز من الرفع.",
          "This browser does not support live camera access. You can still choose an image or use device capture upload."
        )
      );
      return;
    }

    try {
      const nextStream = await navigator.mediaDevices.getUserMedia({
        audio: false,
        video: {
          facingMode: { ideal: "environment" },
          width: { ideal: 1280 },
          height: { ideal: 960 }
        }
      });

      stopMediaStream(streamRef.current);
      streamRef.current = nextStream;
      setCameraOpen(true);
    } catch {
      closeCamera();
      setCameraError(
        getMessage(
          language,
          "لم نتمكن من فتح الكاميرا. تأكد من الإذن أو استخدم اختيار صورة.",
          "Could not open the camera. Check permission or choose an image instead."
        )
      );
    }
  };

  const handleSelectedFile = (selectedFile: File | null, source: CameraCaptureSource): void => {
    if (!selectedFile) {
      return;
    }

    closeCamera();
    setCameraError("");
    setSourceLabel(source);
    onFileChange(selectedFile, source);
  };

  const handleUploadChange = (event: ChangeEvent<HTMLInputElement>, source: CameraCaptureSource): void => {
    handleSelectedFile(event.target.files?.[0] ?? null, source);
    event.target.value = "";
  };

  const handleDrop = (event: DragEvent<HTMLLabelElement>): void => {
    event.preventDefault();
    handleSelectedFile(event.dataTransfer.files?.[0] ?? null, "upload");
  };

  const capturePhoto = (): void => {
    const video = videoRef.current;
    const canvas = canvasRef.current;

    if (!video || !canvas) {
      setCameraError(
        getMessage(language, "تعذر التقاط الصورة. حاول مرة أخرى.", "Could not capture the image. Try again.")
      );
      return;
    }

    const width = video.videoWidth || 1280;
    const height = video.videoHeight || 960;
    canvas.width = width;
    canvas.height = height;

    const context = canvas.getContext("2d");
    if (!context) {
      setCameraError(
        getMessage(language, "تعذر تجهيز الصورة من الكاميرا.", "Could not prepare the camera image.")
      );
      return;
    }

    context.drawImage(video, 0, 0, width, height);
    canvas.toBlob(
      (blob) => {
        if (!blob || blob.size === 0) {
          setCameraError(
            getMessage(language, "الصورة الملتقطة فارغة. أعد المحاولة.", "The captured image is empty. Retake it.")
          );
          return;
        }

        const capturedFile = new File([blob], `olive-sample-${Date.now()}.jpg`, {
          type: "image/jpeg",
          lastModified: Date.now()
        });
        handleSelectedFile(capturedFile, "camera");
      },
      "image/jpeg",
      0.92
    );
  };

  const clearSelection = (): void => {
    closeCamera();
    setCameraError("");
    setSourceLabel(null);
    onFileChange(null, "upload");
  };

  const sourceText =
    sourceLabel === "camera"
      ? getMessage(language, "صورة من الكاميرا", "Camera photo")
      : getMessage(language, "صورة مرفوعة", "Uploaded image");

  return (
    <div className="camera-capture-input">
      <div className="camera-capture-grid">
        <section className={`camera-capture-tile ${cameraOpen ? "is-live" : ""}`}>
          {cameraOpen ? (
            <>
              <video
                aria-label={getMessage(language, "معاينة الكاميرا", "Camera preview")}
                className="camera-live-preview"
                muted
                playsInline
                ref={videoRef}
              />
              <div className="camera-live-actions">
                <button className="btn" disabled={disabled} onClick={capturePhoto} type="button">
                  {getMessage(language, "التقاط الصورة", "Take photo")}
                </button>
                <button className="btn ghost" onClick={closeCamera} type="button">
                  {getMessage(language, "إغلاق", "Close")}
                </button>
              </div>
            </>
          ) : (
            <button className="camera-open-button" disabled={disabled} onClick={() => void openCamera()} type="button">
              <span className="camera-open-icon" aria-hidden="true">
                <svg viewBox="0 0 24 24" focusable="false">
                  <path d="M8.5 6.5 10 4h4l1.5 2.5H19A2.5 2.5 0 0 1 21.5 9v8A2.5 2.5 0 0 1 19 19.5H5A2.5 2.5 0 0 1 2.5 17V9A2.5 2.5 0 0 1 5 6.5h3.5Z" />
                  <path d="M12 16.5a3.5 3.5 0 1 0 0-7 3.5 3.5 0 0 0 0 7Z" />
                </svg>
              </span>
              <strong>{getMessage(language, "افتح الكاميرا", "Open camera")}</strong>
              <small>
                {getMessage(
                  language,
                  "التقط صورة واضحة لعينة الزيتون قبل التنبؤ",
                  "Capture a clear olive sample before prediction"
                )}
              </small>
            </button>
          )}
        </section>

        <label
          className="upload-dropzone camera-upload-tile"
          htmlFor={`${id}-upload`}
          onDragOver={(event) => event.preventDefault()}
          onDrop={handleDrop}
        >
          <input
            accept="image/*"
            disabled={disabled}
            id={`${id}-upload`}
            onChange={(event) => handleUploadChange(event, "upload")}
            type="file"
          />
          <span>{getMessage(language, "رفع صورة يدويًا", "Manual upload")}</span>
          <small>
            {file?.name ?? getMessage(language, "اختر صورة من الجهاز أو اسحبها هنا", "Choose a file or drop it here")}
          </small>
        </label>
      </div>

      <input
        accept="image/*"
        capture="environment"
        className="camera-fallback-input"
        disabled={disabled}
        onChange={(event) => handleUploadChange(event, "camera")}
        ref={cameraFallbackInputRef}
        type="file"
      />
      <canvas className="camera-capture-canvas" ref={canvasRef} />

      {cameraError ? <p className="camera-capture-error">{cameraError}</p> : null}

      <div className="camera-preview-panel">
        {previewUrl ? (
          <>
            <img alt={getMessage(language, "معاينة صورة التنبؤ", "Prediction image preview")} src={previewUrl} />
            <div className="camera-preview-meta">
              <span>{sourceText}</span>
              <strong>{file?.name}</strong>
              <div className="camera-preview-actions">
                <button className="btn secondary" disabled={disabled} onClick={() => void openCamera()} type="button">
                  {getMessage(language, "إعادة التصوير", "Retake")}
                </button>
                <button className="btn ghost" disabled={disabled} onClick={clearSelection} type="button">
                  {getMessage(language, "إزالة الصورة", "Remove")}
                </button>
              </div>
            </div>
          </>
        ) : (
          <div className="camera-preview-empty">
            {getMessage(language, "ستظهر معاينة الصورة هنا قبل تشغيل التنبؤ.", "Preview appears here before prediction.")}
          </div>
        )}
      </div>
    </div>
  );
}
