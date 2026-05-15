# Refactor Toast/Popup for Success & Error Messages

## Step 1: Gather understanding
- Inspect existing message UI (Banner component + where notice/error/success states are used).
- Identify why messages are not visible (currently placed inline in page flow).

## Step 2: Design
- Create a global toast/snackbar component rendered fixed to viewport.
- Support success/danger/neutral tones.
- Auto-dismiss after a few seconds + manual close.
- Keep RTL compatibility.

## Step 3: Implementation
- Add toast component + CSS (fixed position, high z-index).
- Add toast context/hook to trigger toasts from pages.
- Update pages currently using `notice ? <Banner ...>` or `error/success ? <Banner ...>` to instead call toast.
- Keep existing Banner for in-page use if needed.

## Step 4: Styling
- Tune colors/typography to match current theme.
- Ensure it doesn't require scrolling.

## Step 5: Validate
- Build/lint/test (run frontend dev/build).
- Manually verify success/error popups appear and disappear correctly on common flows.

