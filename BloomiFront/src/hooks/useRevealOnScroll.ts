import { useCallback, useRef } from 'react';
import '../styles/scroll-reveal.css';

type Target = { node: HTMLElement; stagger: boolean; start: (delay: number) => void; finish: () => void };
const targets = new Map<HTMLElement, Target>();
let observer: IntersectionObserver | null = null;
let motion: MediaQueryList | null = null;

function motionChanged(event: MediaQueryListEvent) {
  if (event.matches) [...targets.values()].forEach(target => target.finish());
}

function releaseObserver() {
  if (targets.size) return;
  observer?.disconnect();
  observer = null;
  motion?.removeEventListener('change', motionChanged);
  motion = null;
}

function sharedObserver() {
  if (!observer) {
    observer = new IntersectionObserver(entries => {
      // Each visible row/batch gets a short stagger, never a delay proportional
      // to its absolute position in the complete catalog.
      const groups = new Map<HTMLElement | null, number>();
      for (const entry of entries) {
        const target = targets.get(entry.target as HTMLElement);
        if (!entry.isIntersecting || !target || target.node.dataset.scrollReveal !== 'pending') continue;
        const group = target.node.parentElement;
        const index = target.stagger ? groups.get(group) ?? 0 : 0;
        if (target.stagger) groups.set(group, index + 1);
        target.start(Math.min(index, 4) * 70);
      }
    }, { threshold: 0, rootMargin: '0px 0px -24px 0px' });
    motion = window.matchMedia('(prefers-reduced-motion: reduce)');
    motion.addEventListener('change', motionChanged);
  }
  return observer;
}

/** Opt in only on public content. Default rendering stays visible without JS/IO. */
export function useRevealOnScroll<T extends HTMLElement>({ enabled = true, stagger = false } = {}) {
  const revealed = useRef(false);
  return useCallback((node: T | null) => {
    if (!node || !enabled) return;
    let timer: ReturnType<typeof setTimeout> | undefined;
    const stop = () => {
      if (timer !== undefined) clearTimeout(timer);
      node.removeEventListener('focusin', finish);
      node.removeEventListener('animationend', animationEnded);
      observer?.unobserve(node);
      targets.delete(node);
      releaseObserver();
    };
    const finish = () => {
      revealed.current = true;
      node.dataset.scrollReveal = 'done';
      node.style.removeProperty('--reveal-delay');
      stop();
    };
    const animationEnded = (event: AnimationEvent) => {
      if (event.target === node && event.animationName === 'bloomi-scroll-reveal') finish();
    };
    const rect = node.getBoundingClientRect();
    const alreadyVisible = rect.top < window.innerHeight && rect.left < window.innerWidth && rect.right > 0;
    if (revealed.current || alreadyVisible || !('IntersectionObserver' in window) || window.matchMedia('(prefers-reduced-motion: reduce)').matches) {
      revealed.current = true;
      node.dataset.scrollReveal = 'done';
    } else {
      try {
        const shared = sharedObserver();
        targets.set(node, {
          node, stagger, finish,
          start(delay) {
            revealed.current = true;
            observer?.unobserve(node);
            node.style.setProperty('--reveal-delay', `${delay}ms`);
            node.dataset.scrollReveal = 'entering';
            node.addEventListener('animationend', animationEnded);
            // Fail open if an animation is cancelled or overridden by the browser.
            timer = setTimeout(finish, 1100 + delay);
          },
        });
        node.addEventListener('focusin', finish);
        node.dataset.scrollReveal = 'pending';
        shared.observe(node);
      } catch {
        finish();
      }
    }
    return () => {
      stop();
      node.removeAttribute('data-scroll-reveal');
      node.style.removeProperty('--reveal-delay');
    };
  }, [enabled, stagger]);
}
