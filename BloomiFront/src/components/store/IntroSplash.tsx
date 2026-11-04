import { useEffect, useState } from 'react';
import '../../styles/intro-splash.css';

const SESSION_KEY = 'bloomi.intro.seen';
const DURATION_MS = 2500;

function shouldShowIntro() {
  if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return false;
  try {
    return sessionStorage.getItem(SESSION_KEY) !== '1';
  } catch {
    // Skip when session storage is unavailable so navigation cannot repeat it.
    return false;
  }
}

/** One automatic brand introduction per browser-tab session. */
export default function IntroSplash() {
  const [visible, setVisible] = useState(shouldShowIntro);

  useEffect(() => {
    if (!visible) return;
    try {
      sessionStorage.setItem(SESSION_KEY, '1');
    } catch {
      setVisible(false);
      return;
    }
    const motion = window.matchMedia('(prefers-reduced-motion: reduce)');
    const dismissIfReduced = () => { if (motion.matches) setVisible(false); };
    dismissIfReduced();
    motion.addEventListener('change', dismissIfReduced);
    // Also unmount if a background tab or browser delays CSS animation events.
    const timeout = window.setTimeout(() => setVisible(false), DURATION_MS);
    return () => {
      window.clearTimeout(timeout);
      motion.removeEventListener('change', dismissIfReduced);
    };
  }, [visible]);

  if (!visible) return null;
  return <div className="intro-splash" aria-hidden="true" onAnimationEnd={event => {
    if (event.target === event.currentTarget && event.animationName === 'intro-splash-exit') setVisible(false);
  }}>
    <div className="intro-splash-text">
      <p className="intro-splash-brand">Bloomi Bunny</p>
      <div className="intro-splash-words">
        <span className="intro-splash-word intro-splash-buy">Compra</span>
        <span className="intro-splash-word intro-splash-enjoy">Disfruta</span>
        <span className="intro-splash-word intro-splash-bloom">Florece</span>
      </div>
      <p className="intro-splash-tagline">A tu manera <span>♡</span></p>
    </div>
  </div>;
}
