import './globals.css';

export const metadata = {
  title: 'MiniCraft 3D',
  description: 'A lightweight Minecraft-style browser game built with Next.js and Three.js.',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
