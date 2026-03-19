import { Request, Response } from 'express';
import bcrypt from 'bcryptjs';
import jwt, { SignOptions } from 'jsonwebtoken';
import { PrismaClient } from '@prisma/client';
import { config } from '../config/index.js';
import { AppError } from '../middleware/error.js';
import { AuthRequest } from '../middleware/auth.js';

const prisma = new PrismaClient();

export async function register(req: Request, res: Response) {
  const { username, password } = req.body;

  if (!username || !password) {
    throw new AppError(400, 'Username and password required');
  }

  const existing = await prisma.user.findUnique({ where: { username } });
  if (existing) {
    throw new AppError(400, 'Username already exists');
  }

  const passwordHash = await bcrypt.hash(password, 10);
  const user = await prisma.user.create({
    data: { username, passwordHash }
  });

  const token = jwt.sign(
    { userId: String(user.id) },
    config.jwtSecret,
    { expiresIn: config.jwtExpiresIn } as SignOptions
  );

  res.json({ token, user: { id: user.id, username: user.username } });
}

export async function login(req: Request, res: Response) {
  const { username, password } = req.body;

  if (!username || !password) {
    throw new AppError(400, 'Username and password required');
  }

  const user = await prisma.user.findUnique({ where: { username } });
  if (!user) {
    throw new AppError(401, 'Invalid credentials');
  }

  const valid = await bcrypt.compare(password, user.passwordHash);
  if (!valid) {
    throw new AppError(401, 'Invalid credentials');
  }

  const token = jwt.sign(
    { userId: String(user.id) },
    config.jwtSecret,
    { expiresIn: config.jwtExpiresIn } as SignOptions
  );

  res.json({ token, user: { id: user.id, username: user.username } });
}

export async function getMe(req: AuthRequest, res: Response) {
  const user = await prisma.user.findUnique({
    where: { id: req.userId },
    select: { id: true, username: true, createdAt: true }
  });

  if (!user) {
    throw new AppError(404, 'User not found');
  }

  res.json(user);
}
