import { createTheme } from '@mui/material/styles';

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#0b5fff'
    },
    secondary: {
      main: '#ff6b2c'
    },
    background: {
      default: '#f4f7fb',
      paper: '#ffffff'
    },
    success: {
      main: '#14866d'
    },
    warning: {
      main: '#c97300'
    },
    error: {
      main: '#c7384f'
    }
  },
  shape: {
    borderRadius: 18
  },
  typography: {
    fontFamily: '"Space Grotesk", sans-serif',
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
