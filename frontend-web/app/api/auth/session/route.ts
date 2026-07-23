import { NextRequest, NextResponse } from 'next/server'
import { urlAuth } from '../../../../lib/server/backend'

const COOKIE = '__pd_rt'

const cookieOpts = {
  httpOnly: true,
  secure: process.env.NODE_ENV === 'production',
  sameSite: 'strict' as const,
  path: '/api/auth',
  maxAge: 60 * 60 * 24 * 7, // 7 dias — alinhado com o TTL do backend
}

export async function POST(request: NextRequest) {
  const body = await request.json()

  let backendResp: Response
  try {
    backendResp = await fetch(urlAuth('login'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
  } catch {
    return NextResponse.json(
      { message: 'Serviço de autenticação indisponível' },
      { status: 503 },
    )
  }

  const data = await backendResp.json()

  if (!backendResp.ok) {
    return NextResponse.json(data, { status: backendResp.status })
  }

  const { refreshToken, ...rest } = data

  const response = NextResponse.json(rest)
  response.cookies.set(COOKIE, refreshToken, cookieOpts)
  return response
}

export async function DELETE(request: NextRequest) {
  const refreshToken = request.cookies.get(COOKIE)?.value

  if (refreshToken) {
    fetch(urlAuth('logout'), {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken }),
    }).catch(() => {})
  }

  const response = NextResponse.json({ ok: true })
  response.cookies.delete(COOKIE)
  return response
}
