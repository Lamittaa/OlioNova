import { useCallback, useRef, useState } from "react";

export function useActionState() {
  const [busyKey, setBusyKey] = useState<string | null>(null);
  const activeActionsRef = useRef<Map<string, Promise<unknown>>>(new Map());

  const runAction = useCallback(<T>(key: string, action: () => Promise<T>): Promise<T> => {
    const activeAction = activeActionsRef.current.get(key);
    if (activeAction) {
      return activeAction as Promise<T>;
    }

    setBusyKey(key);
    const nextAction = action().finally(() => {
      activeActionsRef.current.delete(key);
      setBusyKey((current) => (current === key ? null : current));
    });

    activeActionsRef.current.set(key, nextAction);
    return nextAction;
  }, []);

  return {
    busyKey,
    isBusy: (key: string) => busyKey === key,
    runAction
  };
}
