const defaultTheme = require('tailwindcss/defaultTheme');

module.exports = {
	darkMode: ['class'],
	content: ['./index.html', './src/**/*.{js,jsx}'],
	theme: {
		extend: {
		// Mantine-style font stack
			fontFamily: {
				sans: [
				'"Inter"',
				'-apple-system',
				'BlinkMacSystemFont',
				'"Segoe UI"',
				'Roboto',
				'"Helvetica Neue"',
				'Arial',
				'sans-serif',
				'"Apple Color Emoji"',
				'"Segoe UI Emoji"'
				],
				display: [
				'"Inter"',
				'-apple-system',
				'BlinkMacSystemFont',
				'"Segoe UI"',
				'Roboto',
				'"Helvetica Neue"',
				'sans-serif'
				]
			},

		// Mantine-forward colors
			colors: {
			// Mantine Blue
				'apple-blue': {
				DEFAULT: '#228BE6',
				light: '#339AF0',
				dark: '#1C7ED6'
				},

			// Mantine Gray scale
				'apple-gray': {
				50: '#F8F9FA',
				100: '#F1F3F5',
				200: '#E9ECEF',
				300: '#DEE2E6',
				400: '#CED4DA',
				500: '#ADB5BD',
				600: '#868E96',
				700: '#495057',
				800: '#343A40',
				900: '#212529',
				950: '#141517'
				},

				// Keep shadcn compatibility
				background: 'hsl(var(--background))',
				foreground: 'hsl(var(--foreground))',
				card: {
					DEFAULT: 'hsl(var(--card))',
					foreground: 'hsl(var(--card-foreground))'
				},
				popover: {
					DEFAULT: 'hsl(var(--popover))',
					foreground: 'hsl(var(--popover-foreground))'
				},
				primary: {
					DEFAULT: 'hsl(var(--primary))',
					foreground: 'hsl(var(--primary-foreground))'
				},
				secondary: {
					DEFAULT: 'hsl(var(--secondary))',
					foreground: 'hsl(var(--secondary-foreground))'
				},
				muted: {
					DEFAULT: 'hsl(var(--muted))',
					foreground: 'hsl(var(--muted-foreground))'
				},
				accent: {
					DEFAULT: 'hsl(var(--accent))',
					foreground: 'hsl(var(--accent-foreground))'
				},
				destructive: {
					DEFAULT: 'hsl(var(--destructive))',
					foreground: 'hsl(var(--destructive-foreground))'
				},
				border: 'hsl(var(--border))',
				input: 'hsl(var(--input))',
				ring: 'hsl(var(--ring))',
			},

		// Mantine-forward Border Radius
			borderRadius: {
			lg: '8px',
			xl: '10px',
			'2xl': '12px',
			'3xl': '16px',
			'apple': '10px'
			},

			// Apple's Spacing (Generous)
			spacing: {
				'18': '4.5rem',   // 72px
				'22': '5.5rem',   // 88px
				'26': '6.5rem',   // 104px
				'30': '7.5rem',   // 120px
				'34': '8.5rem',   // 136px
				'128': '32rem',   // 512px for sections
			},

		// Mantine-forward Shadows
			boxShadow: {
			'apple-sm': '0 1px 2px rgba(0, 0, 0, 0.06)',
			'apple-md': '0 1px 3px rgba(0, 0, 0, 0.1), 0 1px 2px rgba(0, 0, 0, 0.06)',
			'apple-lg': '0 4px 6px rgba(0, 0, 0, 0.1), 0 2px 4px rgba(0, 0, 0, 0.06)',
			'apple-xl': '0 10px 15px rgba(0, 0, 0, 0.1), 0 4px 6px rgba(0, 0, 0, 0.05)',
			'apple-xxl': '0 20px 25px rgba(0, 0, 0, 0.1), 0 10px 10px rgba(0, 0, 0, 0.04)'
			},

		// Mantine-forward Typography Scale
			fontSize: {
			'hero': ['3.5rem', { lineHeight: '1.1', letterSpacing: '-0.01em', fontWeight: '700' }],
			'display-lg': ['3rem', { lineHeight: '1.15', letterSpacing: '-0.01em', fontWeight: '700' }],
			'display': ['2.5rem', { lineHeight: '1.2', letterSpacing: '-0.01em', fontWeight: '700' }],
			'h1': ['2rem', { lineHeight: '1.25', letterSpacing: '-0.005em', fontWeight: '700' }],
			'h2': ['1.5rem', { lineHeight: '1.3', letterSpacing: '-0.005em', fontWeight: '600' }],
			'h3': ['1.25rem', { lineHeight: '1.35', letterSpacing: '-0.002em', fontWeight: '600' }],
			'body-lg': ['1.0625rem', { lineHeight: '1.6', letterSpacing: '0', fontWeight: '400' }],
			'body': ['0.9375rem', { lineHeight: '1.6', letterSpacing: '0', fontWeight: '400' }],
			'body-sm': ['0.875rem', { lineHeight: '1.5', letterSpacing: '0', fontWeight: '400' }],
			'caption': ['0.75rem', { lineHeight: '1.4', letterSpacing: '0', fontWeight: '400' }]
			},

			// Apple's Smooth Animations
			keyframes: {
				'fade-in': {
					'0%': { opacity: '0' },
					'100%': { opacity: '1' },
				},
				'slide-up': {
					'0%': { opacity: '0', transform: 'translateY(20px)' },
					'100%': { opacity: '1', transform: 'translateY(0)' },
				},
				'scale-in': {
					'0%': { opacity: '0', transform: 'scale(0.95)' },
					'100%': { opacity: '1', transform: 'scale(1)' },
				},
			},
			animation: {
				'fade-in': 'fade-in 0.3s ease-out',
				'slide-up': 'slide-up 0.4s ease-out',
				'scale-in': 'scale-in 0.2s ease-out',
			},

			// Apple's Backdrop Blur
			backdropBlur: {
				'apple': '20px',
			},
			maxWidth: {
				'content': '980px',
				'wide': '1200px',
				'ultra': '1400px',
			},
		}
	},
	plugins: [
		require('@tailwindcss/aspect-ratio'),
		require("tailwindcss-animate")
	],
};