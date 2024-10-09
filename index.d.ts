declare module '@totalperspective/pondermatic' {
  interface Pool {
    id: string;
  }
  
  interface Local {
    engine: unknown;
  }

  export type Engine = Pool | Local;

  interface InsertMessage {
    '->db': {
      [key: string]: any;
    };
  }

  interface UpsertMessage {
    '+>db': {
      [key: string]: any;
    };
  }

  export type Message = InsertMessage | UpsertMessage
  export type Task = { __type: 'flow' } & (() => void)

  interface PondermaticAPI {
    createEngine(name: string, resetDb?: boolean): Engine;
    copy(engine: Engine): Engine;
    ruleset(rules: string | object): object;
    dataset(data: string | object): object;
    sh(engine: Engine, msg: Message): void;
    cmd(msg: object): void;
    addRulesMsg(rules: object): object;
    q(engine: Engine, query: string, args: any[], callback: (result: any) => void): Task;
    qP(engine: Engine, query: string, args: any[]): Promise<any>;
    q$(engine: Engine, query: string, ...args: any[]): Promise<any>;
    entity(engine: Engine, ident: string | object, callback: (entity: any) => void): Task;
    entityP(engine: Engine, ident: string | object): Promise<any>;
    watchEntity(engine: Engine, ident: string | object, callback: (entity: any) => void): Task;
    hashId(obj: object): string;
    errorInfo(error: Error): object;
    portal(launcher?: string): void;
    dispose(task: Task): void;
    log(level: string | null, expr: any): void;
    unify(exprOrStr: string | object, env: object | object[]): object;
    pprint(obj: any): void;
    addTap(tap?: (x: any) => void): void;
    readString(str: string): any;
    toString(obj: any): string;
    encode(obj: any): string;
    decode(str: string): any;
    eval(str: string, toJs?: boolean, opts?: object): any;
    toJS(form: any): any;
    devtoolsFormatter: {
      header: (obj: any, config: any) => any;
      hasBody: () => boolean;
      body: (obj: any, config: any) => any;
    };
    stop(engine: Engine): void;
    watchAgents(callback: (agents: any) => void): string;
    removeAgentsWatch(id: string): void;
  }

  const api: PondermaticAPI;
  export default api;
}
