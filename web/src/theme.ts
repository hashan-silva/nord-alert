import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#0a4f86'
    },
    secondary: {
      main: '#d4a514'
    },
    background: {
      default: '#f6f1e6',
      paper: '#fffdf8'
    },
    success: {
      main: '#23715f'
    },
    warning: {
      main: '#b7860b'
    },
    error: {
      main: '#b53a3f'
    }
  },
  shape: {
    borderRadius: 18
  },
  typography: {
    fontFamily: '"Space Grotesk", sans-serif',
    allVariants: {
      color: '#15324b'
    },
    h1: {
      fontSize: '3rem',
      fontWeight: 700
    },
    h2: {
      fontSize: '1.8rem',
      fontWeight: 700
    },
    h3: {
      fontSize: '1.15rem',
      fontWeight: 700
    }
  }
});
