import type { HTMLAttributes } from 'react';
import { useRevealOnScroll } from '../../hooks/useRevealOnScroll';

type Props = HTMLAttributes<HTMLElement> & {
  as?: 'div' | 'section' | 'h1' | 'h2' | 'p';
};

/** Keeps the original semantic element; no extra layout wrapper is inserted. */
export default function RevealOnScroll({ as: Tag = 'div', ...props }: Props) {
  const ref = useRevealOnScroll<HTMLElement>();
  return <Tag {...props} ref={ref}/>;
}
