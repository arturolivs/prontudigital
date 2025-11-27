import js from '@eslint/js'
import typescript from '@typescript-eslint/eslint-plugin'
import typescriptParser from '@typescript-eslint/parser'
import react from 'eslint-plugin-react'
import reactHooks from 'eslint-plugin-react-hooks'
import prettier from 'eslint-plugin-prettier'
import prettierConfig from 'eslint-config-prettier'

export default [
  {
    files: ['**/*.{js,jsx,ts,tsx}'],
    languageOptions: {
      parser: typescriptParser,
      ecmaVersion: 'latest',
      sourceType: 'module',
      parserOptions: {
        ecmaFeatures: {
          jsx: true,
        },
      },
    },
    plugins: {
      '@typescript-eslint': typescript,
      react: react,
      'react-hooks': reactHooks,
      prettier: prettier,
    },
    rules: {
      ...prettierConfig.rules,
      'prettier/prettier': 'error',
      semi: 'off',
      '@typescript-eslint/semi': 'off',
      'react/react-in-jsx-scope': 'off',
    },
  },
  {
    ignores: ['node_modules/', '.next/', 'out/', 'build/'],
  },
]
