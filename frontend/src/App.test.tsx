import { render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it } from 'vitest'
import App from './App'

describe('App', () => {
  it('routes anonymous users to login', async () => {
    localStorage.clear()
    render(<MemoryRouter initialEntries={['/']}><App /></MemoryRouter>)
    expect(await screen.findByText('Welcome back')).toBeTruthy()
  })
})
