import { NextRequest, NextResponse } from 'next/server'

const BACKEND_AUTH =
  process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:9090/api/auth'

const COOKIE = '__pd_rt'

const cookieOpts = {
  httpOnly: true,
  secure: process.env.NODE_ENV === 'production',
  sameSite: 'strict' as const,
  path: '/api/auth',
  maxAge: 60 * 60 * 24 * 7,
}

export async function POST(request: NextRequest) {
  const refreshToken = request.cookies.get(COOKIE)?.value

  if (!refreshToken) {
    return NextResponse.json(
      { message: 'Sessão não encontrada' },
      { status: 401 },
    )
  }

  let backendResp: Response
  try {
    backendResp = await fetch(`${BACKEND_AUTH}/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    })
  } catch {
    return NextResponse.json(
      { message: 'Serviço de autenticação indisponível' },
      { status: 503 },
    )
  }

  if (!backendResp.ok) {
    const response = NextResponse.json(
      { message: 'Sessão expirada' },
      { status: 401 },
    )
    response.cookies.delete(COOKIE)
    return response
  }

  const data = await backendResp.json()
  const { refreshToken: newRefreshToken, ...rest } = data

  const response = NextResponse.json(rest)
  response.cookies.set(COOKIE, newRefreshToken, cookieOpts)
  return response
}
