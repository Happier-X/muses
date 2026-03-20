export const openapi = {
  openapi: "3.0.0",
  info: {
    title: "Muses API",
    description: "音乐流媒体平台 API 文档",
    version: "1.0.0",
  },
  servers: [
    {
      url: "http://localhost:3000",
      description: "开发环境",
    },
  ],
  paths: {
    "/api/auth/register": {
      post: {
        tags: ["认证"],
        summary: "用户注册",
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["username", "password"],
                properties: {
                  username: { type: "string", minLength: 2, description: "用户名" },
                  password: { type: "string", minLength: 6, description: "密码" },
                },
              },
            },
          },
        },
        responses: {
          "200": {
            description: "注册成功",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    user: { $ref: "#/components/schemas/User" },
                    token: { type: "string" },
                    message: { type: "string" },
                  },
                },
              },
            },
          },
          "400": { description: "参数错误" },
          "409": { description: "用户名已存在" },
        },
      },
    },
    "/api/auth/login": {
      post: {
        tags: ["认证"],
        summary: "用户登录",
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["username", "password"],
                properties: {
                  username: { type: "string" },
                  password: { type: "string" },
                },
              },
            },
          },
        },
        responses: {
          "200": {
            description: "登录成功",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    user: { $ref: "#/components/schemas/User" },
                    token: { type: "string" },
                    message: { type: "string" },
                  },
                },
              },
            },
          },
          "401": { description: "用户名或密码错误" },
        },
      },
    },
    "/api/auth/me": {
      get: {
        tags: ["认证"],
        summary: "获取当前用户信息",
        security: [{ BearerAuth: [] }],
        responses: {
          "200": {
            description: "成功",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    user: { $ref: "#/components/schemas/User" },
                  },
                },
              },
            },
          },
          "401": { description: "未登录" },
        },
      },
    },
    "/api/auth/logout": {
      post: {
        tags: ["认证"],
        summary: "退出登录",
        responses: {
          "200": {
            description: "成功",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    message: { type: "string" },
                  },
                },
              },
            },
          },
        },
      },
    },
    "/api/music": {
      get: {
        tags: ["音乐"],
        summary: "获取音乐列表",
        parameters: [
          { name: "page", in: "query", schema: { type: "integer", default: 1 } },
          { name: "limit", in: "query", schema: { type: "integer", default: 50 } },
          { name: "search", in: "query", schema: { type: "string" } },
          { name: "artistId", in: "query", schema: { type: "string" } },
          { name: "albumId", in: "query", schema: { type: "string" } },
        ],
        responses: {
          "200": {
            description: "成功",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    tracks: {
                      type: "array",
                      items: { $ref: "#/components/schemas/Track" },
                    },
                    pagination: { $ref: "#/components/schemas/Pagination" },
                  },
                },
              },
            },
          },
        },
      },
    },
    "/api/music/scan": {
      post: {
        tags: ["音乐"],
        summary: "扫描音乐文件夹",
        security: [{ BearerAuth: [] }],
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["folderPath"],
                properties: {
                  folderPath: { type: "string", description: "音乐文件夹路径" },
                },
              },
            },
          },
        },
        responses: {
          "200": {
            description: "扫描完成",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    success: { type: "boolean" },
                    total: { type: "integer" },
                    added: { type: "integer" },
                    errors: { type: "array", items: { type: "string" } },
                  },
                },
              },
            },
          },
          "401": { description: "需要登录" },
        },
      },
    },
    "/api/music/stream": {
      get: {
        tags: ["音乐"],
        summary: "流式播放音乐",
        parameters: [
          { name: "path", in: "query", required: true, schema: { type: "string" } },
        ],
        responses: {
          "200": {
            description: "音频流",
            content: {
              "audio/mpeg": {},
            },
          },
          "404": { description: "文件未找到" },
        },
      },
    },
    "/api/artists": {
      get: {
        tags: ["艺术家"],
        summary: "获取艺术家列表",
        parameters: [
          { name: "page", in: "query", schema: { type: "integer" } },
          { name: "limit", in: "query", schema: { type: "integer" } },
        ],
        responses: {
          "200": {
            description: "成功",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    artists: {
                      type: "array",
                      items: { $ref: "#/components/schemas/Artist" },
                    },
                    pagination: { $ref: "#/components/schemas/Pagination" },
                  },
                },
              },
            },
          },
        },
      },
    },
    "/api/albums": {
      get: {
        tags: ["专辑"],
        summary: "获取专辑列表",
        parameters: [
          { name: "page", in: "query", schema: { type: "integer" } },
          { name: "limit", in: "query", schema: { type: "integer" } },
        ],
        responses: {
          "200": {
            description: "成功",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    albums: {
                      type: "array",
                      items: { $ref: "#/components/schemas/Album" },
                    },
                    pagination: { $ref: "#/components/schemas/Pagination" },
                  },
                },
              },
            },
          },
        },
      },
    },
    "/api/config/music": {
      get: {
        tags: ["配置"],
        summary: "获取音乐配置",
        responses: {
          "200": {
            description: "成功",
            content: {
              "application/json": {
                schema: {
                  type: "object",
                  properties: {
                    musicFolder: { type: "string" },
                    scanStatus: { type: "string" },
                    lastScanAt: { type: "string", format: "date-time" },
                  },
                },
              },
            },
          },
        },
      },
      put: {
        tags: ["配置"],
        summary: "更新音乐配置",
        security: [{ BearerAuth: [] }],
        requestBody: {
          required: true,
          content: {
            "application/json": {
              schema: {
                type: "object",
                required: ["musicFolder"],
                properties: {
                  musicFolder: { type: "string" },
                },
              },
            },
          },
        },
        responses: {
          "200": { description: "更新成功" },
          "401": { description: "需要登录" },
        },
      },
    },
  },
  components: {
    securitySchemes: {
      BearerAuth: {
        type: "http",
        scheme: "bearer",
        bearerFormat: "JWT",
      },
    },
    schemas: {
      User: {
        type: "object",
        properties: {
          id: { type: "string" },
          username: { type: "string" },
        },
      },
      Track: {
        type: "object",
        properties: {
          id: { type: "string" },
          title: { type: "string" },
          artistName: { type: "string" },
          albumName: { type: "string" },
          duration: { type: "integer" },
          coverUrl: { type: "string" },
          audioUrl: { type: "string" },
        },
      },
      Artist: {
        type: "object",
        properties: {
          id: { type: "string" },
          name: { type: "string" },
          avatar: { type: "string" },
        },
      },
      Album: {
        type: "object",
        properties: {
          id: { type: "string" },
          title: { type: "string" },
          cover: { type: "string" },
          artist: { $ref: "#/components/schemas/Artist" },
        },
      },
      Pagination: {
        type: "object",
        properties: {
          page: { type: "integer" },
          limit: { type: "integer" },
          total: { type: "integer" },
          totalPages: { type: "integer" },
        },
      },
    },
  },
};
