const path = require('path');
const fs = require('fs');
const webpack = require('webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');

const webPackage = JSON.parse(fs.readFileSync(path.resolve(__dirname, 'package.json'), 'utf8'));
const configuredBackendVersion = process.env.REACT_APP_BACKEND_VERSION;
const backendPomPath = path.resolve(__dirname, '../backend/pom.xml');

function resolveBackendVersion() {
  if (configuredBackendVersion && configuredBackendVersion.length > 0) {
    return configuredBackendVersion;
  }

  if (!fs.existsSync(backendPomPath)) {
    return 'unknown';
  }

  const backendPom = fs.readFileSync(backendPomPath, 'utf8');
  const artifactIdTag = '<artifactId>backend</artifactId>';
  const versionStartTag = '<version>';
  const versionEndTag = '</version>';
  const artifactIdIndex = backendPom.indexOf(artifactIdTag);

  if (artifactIdIndex === -1) {
    return 'unknown';
  }

  const versionStartIndex = backendPom.indexOf(versionStartTag, artifactIdIndex + artifactIdTag.length);
  if (versionStartIndex === -1) {
    return 'unknown';
  }

  const versionValueStart = versionStartIndex + versionStartTag.length;
  const versionEndIndex = backendPom.indexOf(versionEndTag, versionValueStart);
  if (versionEndIndex === -1) {
    return 'unknown';
  }

  const version = backendPom.slice(versionValueStart, versionEndIndex).trim();
  return version.length > 0 ? version : 'unknown';
}

const backendVersion = resolveBackendVersion();
const publicPath = path.resolve(__dirname, 'public');

class StaticAssetsPlugin {
  apply(compiler) {
    compiler.hooks.afterEmit.tap('StaticAssetsPlugin', () => {
      if (!fs.existsSync(publicPath)) {
        return;
      }

      fs.cpSync(publicPath, compiler.options.output.path, {
        recursive: true
      });
    });
  }
}

module.exports = (_, argv) => ({
  entry: path.resolve(__dirname, 'src/main.tsx'),
  output: {
    path: path.resolve(__dirname, 'dist'),
    filename: argv.mode === 'production' ? 'assets/[name].[contenthash].js' : 'assets/[name].js',
    clean: true
  },
  resolve: {
    extensions: ['.ts', '.tsx', '.js', '.jsx']
  },
  module: {
    rules: [
      {
        test: /\.(ts|tsx|js|jsx)$/,
        exclude: /node_modules/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: [
              ['@babel/preset-env', { targets: 'defaults' }],
              ['@babel/preset-react', { runtime: 'automatic' }],
              ['@babel/preset-typescript']
            ]
          }
        }
      },
      {
        test: /\.scss$/,
        use: ['style-loader', 'css-loader', 'sass-loader']
      }
    ]
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: path.resolve(__dirname, 'index.html')
    }),
    new StaticAssetsPlugin(),
    new webpack.DefinePlugin({
      'process.env.REACT_APP_BACKEND_BASE_URL': JSON.stringify(
        process.env.REACT_APP_BACKEND_BASE_URL || ''
      ),
      'process.env.REACT_APP_WEB_VERSION': JSON.stringify(webPackage.version),
      'process.env.REACT_APP_BACKEND_VERSION': JSON.stringify(backendVersion)
    })
  ],
  devServer: {
    static: path.resolve(__dirname, 'public'),
    historyApiFallback: true,
    port: 3000
  }
});
